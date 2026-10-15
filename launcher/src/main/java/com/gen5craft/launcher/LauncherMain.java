package com.gen5craft.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Properties;

/** Gen5Craft Launcher (Windows-first, Java, no external dependencies).
 *  Reads gen5craft-manifest.json (next to the launcher, or from a URL),
 *  downloads Gen5Craft.jar with SHA-256 verification (skipping valid files),
 *  and launches the client. Version checks and installs never block the UI. */
public final class LauncherMain extends JFrame {
    private final Path installDir;
    private final Path clientJar;
    private final JTextField manifestUrl = new JTextField(
        "https://github.com/obsifox/Gen5Craft/releases/download/v1.5.0/gen5craft-manifest.json");
    private final JLabel status = new JLabel("Ready.");
    private final JProgressBar bar = new JProgressBar(0, 100);
    private final JLabel installed = new JLabel("Installed: -");
    // Local profile (Phase 9 revised): name + skin + cape, shown on the profile card.
    private Profile profile = new Profile();
    private Path profileFile;
    private JLabel headLabel, profileInfo, capeSwatch, skinPathLabel;
    private JTextField profileNameField;
    private JComboBox<String> skinCombo, capeCombo;
    private String chosenSkinPath = "";

    public LauncherMain() {
        super("Gen5Craft Launcher");
        String appData = System.getenv("APPDATA");
        installDir = (System.getProperty("os.name", "").toLowerCase().contains("win") && appData != null)
                ? Paths.get(appData, "Gen5Craft") : Paths.get(System.getProperty("user.home"), ".gen5craft");
        clientJar = installDir.resolve("client/Gen5Craft.jar");
        profileFile = installDir.resolve("profile.json");
        profile = Profile.load(profileFile);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 640));
        setSize(980, 700);
        setLocationRelativeTo(null);
        buildUi();
        refreshInstalled();
        refreshProfileCard();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(24, 42, 74), 0, getHeight(), new Color(178, 104, 54)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        setContentPane(root);

        JLabel logo = new JLabel("MINECRAFT");
        logo.setFont(new Font("SansSerif", Font.BOLD, 24));
        logo.setForeground(new Color(255, 255, 255, 225));
        JLabel title = new JLabel("Gen5Craft Launcher 1.5.0  |  Gen 5 (1.20.5 - 26.3)  |  Java 25 / Windows");
        title.setForeground(new Color(220, 235, 250));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false); top.setBorder(new EmptyBorder(14, 16, 6, 16));
        top.add(logo, BorderLayout.WEST); top.add(title, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        JPanel glass = new JPanel(new BorderLayout(10, 10)) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(8, 12, 18, 178)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(255, 255, 255, 55)); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose(); super.paintComponent(g);
            }
        };
        glass.setOpaque(false);
        glass.setBorder(new EmptyBorder(22, 26, 22, 26));
        JPanel center = new JPanel(new GridBagLayout()); center.setOpaque(false); center.add(glass);
        root.add(center, BorderLayout.CENTER);

        // ---- Profile card: head (default Steve), name, skin & cape controls ----
        JPanel profPanel = new JPanel(new BorderLayout(14, 6)); profPanel.setOpaque(false);
        headLabel = new JLabel();
        profPanel.add(headLabel, BorderLayout.WEST);
        profileInfo = new JLabel();
        profileInfo.setForeground(Color.WHITE);
        profileInfo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel mid = new JPanel(new BorderLayout(4, 4)); mid.setOpaque(false);
        JLabel profTitle = new JLabel("Local Profile (offline account)");
        profTitle.setFont(new Font("SansSerif", Font.BOLD, 16)); profTitle.setForeground(Color.WHITE);
        mid.add(profTitle, BorderLayout.NORTH);
        mid.add(profileInfo, BorderLayout.CENTER);
        profPanel.add(mid, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(0, 1, 6, 6)); controls.setOpaque(false);
        profileNameField = new JTextField(profile.name, 14);
        skinCombo = new JComboBox<String>(new String[]{"Default Steve (from game data)", "Custom skin PNG..."});
        JButton chooseSkin = new JButton("Choose skin PNG");
        chooseSkin.addActionListener(e -> chooseSkinFile());
        skinPathLabel = new JLabel("");
        skinPathLabel.setForeground(new Color(200, 225, 255));
        capeCombo = new JComboBox<String>();
        for (Profile.Cape c : Profile.Cape.values()) capeCombo.addItem(c.label);
        capeSwatch = new JLabel();
        JButton saveProfile = new JButton("Save Profile");
        saveProfile.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveProfile.addActionListener(e -> saveProfile());
        controls.add(row("Profile name:", profileNameField));
        controls.add(row("Skin:", skinCombo, chooseSkin));
        controls.add(skinPathLabel);
        controls.add(row("Cape:", capeCombo, capeSwatch));
        controls.add(row("", saveProfile));
        profPanel.add(controls, BorderLayout.EAST);
        glass.add(profPanel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 1, 10, 10)); form.setOpaque(false);
        JLabel m = new JLabel("Manifest URL (or keep the local gen5craft-manifest.json next to the launcher):");
        m.setForeground(Color.WHITE); form.add(m);
        form.add(manifestUrl);
        installed.setForeground(new Color(200, 225, 255)); form.add(installed);
        status.setForeground(new Color(235, 242, 250)); form.add(status);
        bar.setStringPainted(true); form.add(bar);
        glass.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6)); buttons.setOpaque(false);
        JButton play = big("Play");
        JButton update = big("Check / Update client");
        JButton open = big("Open game folder");
        play.addActionListener(e -> play());
        update.addActionListener(e -> updateClient());
        open.addActionListener(e -> openFolder());
        buttons.add(open); buttons.add(update); buttons.add(play);
        glass.add(buttons, BorderLayout.SOUTH);
    }
    private JPanel row(String label, Component... comps) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2)); r.setOpaque(false);
        JLabel l = new JLabel(label); l.setForeground(Color.WHITE); l.setPreferredSize(new Dimension(86, 22));
        r.add(l);
        for (Component c : comps) r.add(c);
        return r;
    }
    private void chooseSkinFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a 64x64 Minecraft skin PNG");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            chosenSkinPath = fc.getSelectedFile().getAbsolutePath();
            skinCombo.setSelectedIndex(1);
            skinPathLabel.setText(fc.getSelectedFile().getName());
        }
    }
    private void saveProfile() {
        String name = Profile.sanitize(profileNameField.getText());
        if (name == null) { status.setText("Invalid profile name: use 3-16 letters, digits or underscore."); return; }
        profile.name = name;
        profile.skinPath = skinCombo.getSelectedIndex() == 1 ? chosenSkinPath : "";
        if (skinCombo.getSelectedIndex() == 1 && profile.skinPath.isEmpty()) {
            status.setText("Pick a skin PNG first, or switch back to Default Steve.");
            return;
        }
        profile.cape = Profile.Cape.values()[capeCombo.getSelectedIndex()];
        try {
            profile.save(profileFile);
            refreshProfileCard();
            status.setText("Profile saved: " + profile.name + " (local account, stored on this PC only).");
        } catch (Exception ex) { status.setText("Profile save failed: " + ex.getMessage()); }
    }
    private void refreshProfileCard() {
        if (headLabel == null) return;
        SkinTextures.Loaded loaded = SkinTextures.loadSkin(installDir, profile);
        headLabel.setIcon(new ImageIcon(SkinTextures.headCube(loaded.skin, 104)));
        String uuid = profile.offlineUuid().toString();
        profileInfo.setText("<html><b><font size=+1>" + profile.name + "</font></b> &nbsp;(local account)<br>"
                + "Offline UUID: " + uuid.substring(0, 8) + "..." + uuid.substring(uuid.length() - 4) + "<br>"
                + "Skin: " + loaded.source + "<br>"
                + "Cape: " + profile.cape.label + " (cosmetic, local only)</html>");
        capeSwatch.setIcon(new ImageIcon(SkinTextures.capePreview(profile.cape, 30, 40)));
        profileNameField.setText(profile.name);
        skinCombo.setSelectedIndex(profile.skinPath == null || profile.skinPath.isEmpty() ? 0 : 1);
        chosenSkinPath = profile.skinPath == null ? "" : profile.skinPath;
        skinPathLabel.setText(chosenSkinPath.isEmpty() ? "" : Paths.get(chosenSkinPath).getFileName().toString());
        capeCombo.setSelectedIndex(profile.cape.ordinal());
    }

    private JButton big(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("SansSerif", Font.BOLD, 15)); b.setForeground(Color.WHITE);
        b.setBackground(new Color(38, 110, 60)); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        return b;
    }

    private void refreshInstalled() {
        try {
            if (Files.exists(clientJar)) installed.setText("Installed: " + clientJar + " (" + Files.size(clientJar) + " bytes)");
            else installed.setText("Installed: not yet - click Check / Update client.");
        } catch (Exception ignored) {}
    }

    public static int compareVersions(String a, String b) {
        String[] pa = a.replaceAll("[^0-9.]", "").split("\\."), pb = b.replaceAll("[^0-9.]", "").split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int va = i < pa.length && !pa[i].isEmpty() ? Integer.parseInt(pa[i]) : 0;
            int vb = i < pb.length && !pb[i].isEmpty() ? Integer.parseInt(pb[i]) : 0;
            if (va != vb) return va - vb;
        }
        return 0;
    }
    private boolean verifyInstalled() {
        try {
            Path installedManifest = installDir.resolve("client/installed-manifest.json");
            if (!Files.exists(installedManifest)) return true; // nothing recorded yet
            String text = new String(Files.readAllBytes(installedManifest), StandardCharsets.UTF_8);
            String expected = extractSha(text, "Gen5Craft.jar");
            return expected == null || expected.equalsIgnoreCase(sha256(clientJar));
        } catch (Exception e) { return false; }
    }
    private void play() {
        if (!Files.exists(clientJar)) { status.setText("Client is not installed yet. Run Check / Update client first."); return; }
        if (!verifyInstalled()) { status.setText("Integrity check FAILED: installed client does not match its manifest SHA-256. Run Check / Update client."); return; }
        try {
            String javaBin = Paths.get(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
            new ProcessBuilder(javaBin, "-Xmx2G", "-jar", clientJar.toString())
                    .directory(installDir.toFile()).start();
            status.setText("Gen5Craft started.");
        } catch (Exception ex) { status.setText("Start failed: " + ex.getMessage()); }
    }
    private void openFolder() {
        try { Files.createDirectories(installDir); Desktop.getDesktop().open(installDir.toFile()); }
        catch (Exception ex) { status.setText("Open failed: " + ex.getMessage()); }
    }

    private void updateClient() {
        bar.setValue(0); status.setText("Resolving manifest...");
        new SwingWorker<Void, String>() {
            protected Void doInBackground() throws Exception {
                String manifest = loadManifest();
                String name = extractFileName(manifest, "Gen5Craft.jar");
                String sha = extractSha(manifest, "Gen5Craft.jar");
                if (name == null || sha == null) throw new IllegalStateException("Gen5Craft.jar not found in manifest.");
                String base = manifestBase();
                Path dest = clientJar;
                if (Files.exists(dest) && sha.equalsIgnoreCase(sha256(dest))) { publish("Client already up to date (SHA-256 OK)."); setProgress(100); return null; }
                publish("Downloading Gen5Craft.jar ...");
                download(base + "Gen5Craft.jar", dest);
                if (!sha.equalsIgnoreCase(sha256(dest))) throw new IllegalStateException("SHA-256 mismatch after download.");
                Files.write(installDir.resolve("client/installed-manifest.json"), manifest.getBytes(StandardCharsets.UTF_8));
                publish("Updated and verified: " + dest);
                setProgress(100);
                return null;
            }
            protected void process(java.util.List<String> chunks) { status.setText(chunks.get(chunks.size() - 1)); }
            protected void done() {
                try { get(); } catch (Exception ex) { status.setText("Update failed: " + rootMsg(ex) + "  (private draft releases need a public manifest URL or a local manifest file)"); }
                refreshInstalled();
            }
        }.execute();
        // progress is coarse for this lightweight launcher (manifest + one jar)
        bar.setValue(50);
    }
    private String rootMsg(Throwable t) { while (t.getCause() != null) t = t.getCause(); return String.valueOf(t.getMessage()); }
    private String manifestBase() {
        String u = manifestUrl.getText().trim();
        int i = u.lastIndexOf('/');
        return i > 0 ? u.substring(0, i + 1) : u;
    }
    private String loadManifest() throws Exception {
        Path local = Paths.get(System.getProperty("user.dir"), "gen5craft-manifest.json");
        if (Files.exists(local)) { status.setText("Using local manifest: " + local); return new String(Files.readAllBytes(local), StandardCharsets.UTF_8); }
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(manifestUrl.getText().trim()))
                .header("User-Agent", "Gen5Craft-Launcher/1.5.0").GET().build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IllegalStateException("Manifest HTTP " + res.statusCode());
        return res.body();
    }
    private void download(String url, Path dest) throws Exception {
        Files.createDirectories(dest.getParent());
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<InputStream> res = http.send(HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Gen5Craft-Launcher/1.5.0").GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) throw new IllegalStateException("Download HTTP " + res.statusCode() + " for " + url);
        Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".part");
        InputStream in = res.body(); OutputStream out = Files.newOutputStream(tmp);
        try { byte[] buf = new byte[64 * 1024]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n); }
        finally { out.close(); in.close(); }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }
    public static String extractFileName(String manifest, String wanted) { return manifest.contains(wanted) ? wanted : null; }
    public static String extractSha(String manifest, String wanted) {
        int i = manifest.indexOf(wanted); if (i < 0) return null;
        int s = manifest.indexOf("\"sha256\": \"", i); if (s < 0) return null;
        int start = s + 11; int e = manifest.indexOf('"', start);
        return e < 0 ? null : manifest.substring(start, e);
    }
    public static String sha256(Path f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        InputStream in = Files.newInputStream(f);
        try { byte[] buf = new byte[64 * 1024]; int n; while ((n = in.read(buf)) != -1) md.update(buf, 0, n); } finally { in.close(); }
        StringBuilder sb = new StringBuilder(); for (byte b : md.digest()) sb.append(String.format("%02x", b)); return sb.toString();
    }
    public static void main(String[] args) {
        Properties p = new Properties(); // reserved for persisted launcher options
        SwingUtilities.invokeLater(new Runnable() { public void run() { new LauncherMain().setVisible(true); } });
    }
}
