package com.gen5craft.ui;

import com.gen5craft.core.FeatureGate;
import com.gen5craft.core.GameDownloader;
import com.gen5craft.core.Gen5Versions;
import com.gen5craft.core.ServerPinger;
import com.gen5craft.net.LoginSession;
import com.gen5craft.core.PerfProfile;
import com.gen5craft.core.Settings;
import com.gen5craft.core.WorldModel;
import com.gen5craft.render.CameraState;
import com.gen5craft.render.SoftwareRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Main menu, exactly as designed:
 *  - Panorama background (pre-rendered, cached; static on weak systems)
 *  - Small Minecraft logo bottom-left, version bottom-right
 *  - Fully transparent header, buttons ordered right-to-left:
 *    Language | Multiplayer | Singleplayer | Settings | Exit
 *  - Selecting a tab opens a dark glass panel in the center over the panorama. */
public final class MainMenuFrame extends JFrame {
    private final I18n i18n = new I18n();
    private final Settings settings;
    private final Path settingsFile;
    private final Path gameDir;

    private PanoramaPanel panorama;
    private JPanel glassCardHolder;
    private CardLayout cards;
    private final List<JButton> headerButtons = new ArrayList<JButton>();
    private JButton bLang, bMulti, bSingle, bSettings, bExit;
    private JLabel versionLabel;
    private boolean multiBuilt, singleBuilt, settingsBuilt, langBuilt;

    public MainMenuFrame(Settings settings, Path settingsFile, Path gameDir) {
        super("Gen5Craft");
        this.settings = settings; this.settingsFile = settingsFile; this.gameDir = gameDir;
        i18n.setLanguage(settings.language);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 540));
        setSize(1280, 720);
        setLocationRelativeTo(null);
        buildUi();
        showTab("single");
    }

    private void buildUi() {
        panorama = new PanoramaPanel(settings);
        panorama.setLayout(new BorderLayout());
        setContentPane(panorama);

        // Header: fully transparent, RTL button order.
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        header.setOpaque(false);
        applyOrientation(header);
        bLang = headerButton("language", "lang");
        bMulti = headerButton("multiplayer", "multi");
        bSingle = headerButton("singleplayer", "single");
        bSettings = headerButton("settings", "settings");
        bExit = headerButton("exit", "exit");
        // Added in this order; with RIGHT_TO_LEFT orientation the first button
        // (Language) is placed at the far right, exactly as designed.
        header.add(bLang); header.add(bMulti); header.add(bSingle); header.add(bSettings); header.add(bExit);
        panorama.add(header, BorderLayout.NORTH);

        // Center dark-glass area.
        cards = new CardLayout();
        glassCardHolder = new JPanel(cards);
        glassCardHolder.setOpaque(false);
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        JPanel glass = new GlassPanel();
        glass.setLayout(new BorderLayout());
        glass.setPreferredSize(new Dimension(760, 430));
        glass.add(glassCardHolder, BorderLayout.CENTER);
        centerWrap.add(glass);
        panorama.add(centerWrap, BorderLayout.CENTER);

        // Bottom bar: logo left, version right.
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(6, 14, 8, 14));
        JLabel logo = new JLabel("MINECRAFT");
        logo.setFont(new Font("SansSerif", Font.BOLD, 26));
        logo.setForeground(new Color(255, 255, 255, 225));
        JLabel sub = new JLabel("GEN5CRAFT");
        sub.setFont(new Font("SansSerif", Font.BOLD, 11));
        sub.setForeground(new Color(180, 220, 255, 200));
        JPanel logoBox = new JPanel(new BorderLayout()); logoBox.setOpaque(false);
        logoBox.add(logo, BorderLayout.CENTER); logoBox.add(sub, BorderLayout.SOUTH);
        versionLabel = new JLabel("Gen5Craft 1.1.0  |  Minecraft Java " + Gen5Versions.latest()
                + "  |  Gen 5 (" + Gen5Versions.first() + " - " + Gen5Versions.latest() + ")");
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(255, 255, 255, 190));
        bottom.add(logoBox, BorderLayout.WEST);
        bottom.add(versionLabel, BorderLayout.EAST);
        panorama.add(bottom, BorderLayout.SOUTH);
    }

    private void applyOrientation(JComponent c) {
        c.setComponentOrientation(i18n.rtl() ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
    }

    private JButton headerButton(final String key, final String tab) {
        final JButton b = new JButton(i18n.t(key));
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(10, 16, 24, 110));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 60)),
                new EmptyBorder(8, 18, 8, 18)));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ("exit".equals(tab)) { confirmExit(); return; }
                showTab(tab);
            }
        });
        headerButtons.add(b);
        return b;
    }

    private void showTab(String tab) {
        if ("single".equals(tab) && !singleBuilt) { glassCardHolder.add(buildSingle(), "single"); singleBuilt = true; }
        if ("multi".equals(tab) && !multiBuilt) { glassCardHolder.add(buildMulti(), "multi"); multiBuilt = true; }
        if ("settings".equals(tab) && !settingsBuilt) { glassCardHolder.add(buildSettings(), "settings"); settingsBuilt = true; }
        if ("lang".equals(tab) && !langBuilt) { glassCardHolder.add(buildLanguage(), "lang"); langBuilt = true; }
        cards.show(glassCardHolder, tab);
    }

    private JPanel baseTab(String titleKey) {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(18, 22, 18, 22));
        JLabel t = new JLabel(i18n.t(titleKey));
        t.setFont(new Font("SansSerif", Font.BOLD, 20));
        t.setForeground(Color.WHITE);
        applyOrientation(t);
        p.add(t, BorderLayout.NORTH);
        return p;
    }
    private JLabel white(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(235, 242, 250));
        l.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return l;
    }

    // ---- Singleplayer: local worlds always run on the latest (26.3) data.
    private JPanel buildSingle() {
        JPanel p = baseTab("singleplayer");
        final DefaultListModel<String> model = new DefaultListModel<String>();
        refreshWorlds(model);
        JList<String> list = new JList<String>(model);
        list.setBackground(new Color(255, 255, 255, 18));
        list.setForeground(Color.WHITE);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT)); row.setOpaque(false);
        final JProgressBar bar = new JProgressBar(0, 100); bar.setStringPainted(true); bar.setVisible(false);
        JButton create = new JButton(i18n.t("newWorld"));
        create.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // Phase 2.3: worlds are created through the WorldModel
                    // (metadata pinned to the latest Gen-5 data version).
                    WorldModel.create(gameDir.resolve("saves"), "World");
                    refreshWorlds(model);
                } catch (Exception ex) { showError(ex); }
            }
        });
        JButton download = new JButton(i18n.t("downloadData"));
        download.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { startDownload(bar); }
        });
        JButton playWorld = new JButton("Play World (Phase 6)");
        playWorld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sel = list.getSelectedValue();
                String world = (sel == null || sel.equals(i18n.t("worldsEmpty"))) ? "Preview World" : sel.split("  \\[")[0];
                openRendererPreview(world);
            }
        });
        row.add(playWorld);
        JButton preview3d = new JButton("3D Preview (Phase 3)");
        preview3d.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { openRendererPreview("Preview World"); }
        });
        row.add(create); row.add(download); row.add(preview3d);
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(row, BorderLayout.NORTH); south.add(bar, BorderLayout.SOUTH);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }
    private void refreshWorlds(DefaultListModel<String> model) {
        model.clear();
        try {
            for (WorldModel w : WorldModel.list(gameDir.resolve("saves"))) {
                model.addElement(w.name + "  [" + w.dataVersion + "]");
            }
        } catch (Exception ignored) {}
        if (model.isEmpty()) model.addElement(i18n.t("worldsEmpty"));
    }
    private void startDownload(final JProgressBar bar) {
        bar.setVisible(true); bar.setValue(0); bar.setString("...");
        SwingWorker<Void, String> w = new SwingWorker<Void, String>() {
            protected Void doInBackground() throws Exception {
                GameDownloader dl = new GameDownloader();
                publish("Resolving " + Gen5Versions.latest() + " in Mojang manifest...");
                final GameDownloader.Plan plan = dl.buildPlan(Gen5Versions.latest());
                dl.downloadCore(plan, gameDir, new GameDownloader.Progress() {
                    public void on(String what, int percent) { publish(what); setProgress(percent); }
                });
                return null;
            }
            protected void process(List<String> chunks) { bar.setString(chunks.get(chunks.size() - 1)); }
            protected void done() {
                try { get(); bar.setValue(100); bar.setString("OK - " + Gen5Versions.latest()); }
                catch (Exception ex) { bar.setString("Error"); showError(ex); }
            }
        };
        w.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent ev) {
                if ("progress".equals(ev.getPropertyName())) bar.setValue((Integer) ev.getNewValue());
            }
        });
        w.execute();
    }

    // ---- Multiplayer: Gen-5 check + disabling newer content for older servers.
    private JPanel buildMulti() {
        JPanel p = baseTab("multiplayer");
        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8)); form.setOpaque(false);
        form.add(white(i18n.t("serverAddress")));
        final JTextField address = new JTextField("mc.example.net:25565");
        form.add(address);
        form.add(white(i18n.t("serverVersion")));
        final JComboBox<String> versions = new JComboBox<String>();
        for (Gen5Versions.Entry e : Gen5Versions.all()) versions.addItem(e.id);
        versions.setSelectedItem(Gen5Versions.latest());
        form.add(versions);
        final JTextArea out = new JTextArea(7, 40);
        out.setEditable(false); out.setLineWrap(true); out.setWrapStyleWord(true);
        JButton check = new JButton(i18n.t("checkServer"));
        check.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FeatureGate.Profile prof = FeatureGate.forServer((String) versions.getSelectedItem());
                StringBuilder sb = new StringBuilder();
                sb.append(address.getText()).append("\n");
                sb.append(prof.reason).append("\n");
                if (prof.allowed && !prof.disabledFeatures.isEmpty()) {
                    sb.append("Disabled on this client for that server:\n");
                    for (String f : prof.disabledFeatures) sb.append(" - ").append(f).append("\n");
                }
                out.setText(sb.toString());
            }
        });
        form.add(check);
        final JTextField userField = new JTextField("Gen5Player");
        form.add(white("Username (offline-mode)"));
        form.add(userField);
        JButton loginBtn = new JButton("Login (offline) -> Configuration -> Play");
        loginBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final String version = (String) versions.getSelectedItem();
                com.gen5craft.core.Gen5Versions.Entry entry = com.gen5craft.core.Gen5Versions.find(version);
                final int protocol = entry != null && entry.protocol > 0 ? entry.protocol : 766;
                out.setText("Starting Gen-5 login session (protocol " + protocol + ")...");
                new SwingWorker<LoginSession.Result, String>() {
                    protected LoginSession.Result doInBackground() throws Exception {
                        return new LoginSession().connect(address.getText(), protocol, version,
                                userField.getText(), 6000, new LoginSession.Listener() {
                                    public void on(String line) { publish(line); }
                                });
                    }
                    protected void process(java.util.List<String> chunks) {
                        StringBuilder sb = new StringBuilder(out.getText());
                        for (String c : chunks) sb.append("\n").append(c);
                        out.setText(sb.toString());
                    }
                    protected void done() {
                        try {
                            LoginSession.Result r = get();
                            out.setText(out.getText() + "\n== " + r.detail);
                        } catch (Exception ex) {
                            out.setText(out.getText() + "\nLogin failed: " + ex);
                        }
                    }
                }.execute();
            }
        });
        form.add(loginBtn);
        JButton pingBtn = new JButton("Ping (Gen-5 status)");
        pingBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.setText("Pinging " + address.getText() + " ...");
                new SwingWorker<ServerPinger.Status, Void>() {
                    protected ServerPinger.Status doInBackground() throws Exception {
                        return ServerPinger.ping(address.getText(), 5000);
                    }
                    protected void done() {
                        try {
                            ServerPinger.Status st = get();
                            StringBuilder sb = new StringBuilder();
                            sb.append(address.getText()).append("  (").append(st.latencyMs).append(" ms)\n");
                            sb.append("Version: ").append(st.versionName).append("  Protocol: ").append(st.protocol).append("\n");
                            sb.append("Players: ").append(st.online).append("/").append(st.max).append("\n");
                            if (st.description.length() > 0) sb.append(st.description).append("\n");
                            if (st.profile != null) sb.append(st.profile.reason).append("\n");
                            out.setText(sb.toString());
                        } catch (Exception ex) {
                            out.setText("Ping failed: " + ex);
                        }
                    }
                }.execute();
            }
        });
        form.add(pingBtn);
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(out), BorderLayout.CENTER);
        return p;
    }

    // ---- Settings, including the GPU section (Auto/integrated/dedicated/CPU).
    private JPanel buildSettings() {
        JPanel p = baseTab("settings");
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10)); form.setOpaque(false);
        form.add(white(i18n.t("gpuSettings") + " — " + i18n.t("gpuMode")));
        final JComboBox<String> gpu = new JComboBox<String>(new String[]{
                i18n.t("gpuAuto"), i18n.t("gpuIntegrated"), i18n.t("gpuDedicated"), i18n.t("gpuCpuSoftware")});
        gpu.setSelectedIndex(settings.gpuMode.ordinal());
        form.add(gpu);
        form.add(white(i18n.t("quality")));
        final JComboBox<String> quality = new JComboBox<String>(new String[]{"AUTO", "PERFORMANCE", "BALANCED", "QUALITY"});
        quality.setSelectedItem(settings.quality.name());
        form.add(quality);
        form.add(white(i18n.t("renderScale") + " %"));
        final JSlider scale = new JSlider(50, 100, settings.renderScalePercent);
        scale.setOpaque(false);
        form.add(scale);
        form.add(white(i18n.t("fpsLimit")));
        final JComboBox<String> fps = new JComboBox<String>(new String[]{"30", "60", "120", "Unlimited"});
        fps.setSelectedItem(settings.fpsLimit == 0 ? "Unlimited" : Integer.toString(settings.fpsLimit));
        form.add(fps);
        form.add(white(i18n.t("panoramaAnim")));
        final JCheckBox anim = new JCheckBox();
        anim.setOpaque(false); anim.setSelected(settings.panoramaAnimation);
        form.add(anim);
        form.add(white(settings.systemSummary() + " | Effective: " + settings.effectiveQuality()));
        final JLabel status = white("");
        form.add(status);
        p.add(form, BorderLayout.CENTER);
        JButton save = new JButton(i18n.t("save"));
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                settings.gpuMode = Settings.GpuMode.values()[gpu.getSelectedIndex()];
                settings.quality = Settings.Quality.valueOf((String) quality.getSelectedItem());
                settings.renderScalePercent = scale.getValue();
                settings.fpsLimit = "Unlimited".equals(fps.getSelectedItem()) ? 0 : Integer.parseInt((String) fps.getSelectedItem());
                settings.panoramaAnimation = anim.isSelected();
                try { settings.save(settingsFile); status.setText(i18n.t("saved") + " (GPU mode applies fully after restart)"); }
                catch (Exception ex) { showError(ex); }
            }
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT)); south.setOpaque(false); south.add(save);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // ---- Language: FA = RTL, EN = LTR.
    private JPanel buildLanguage() {
        JPanel p = baseTab("language");
        JPanel box = new JPanel(new GridLayout(0, 1, 8, 8)); box.setOpaque(false);
        final JRadioButton fa = new JRadioButton("فارسی (راست‌به‌چپ)");
        final JRadioButton en = new JRadioButton("English (LTR)");
        fa.setOpaque(false); en.setOpaque(false);
        fa.setForeground(Color.WHITE); en.setForeground(Color.WHITE);
        ButtonGroup g = new ButtonGroup(); g.add(fa); g.add(en);
        if (i18n.rtl()) fa.setSelected(true); else en.setSelected(true);
        JButton apply = new JButton(i18n.t("save"));
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                settings.language = en.isSelected() ? "en" : "fa";
                i18n.setLanguage(settings.language);
                bLang.setText(i18n.t("language")); bMulti.setText(i18n.t("multiplayer"));
                bSingle.setText(i18n.t("singleplayer")); bSettings.setText(i18n.t("settings")); bExit.setText(i18n.t("exit"));
                try { settings.save(settingsFile); } catch (Exception ex) { showError(ex); }
                JOptionPane.showMessageDialog(MainMenuFrame.this, i18n.t("saved"));
            }
        });
        box.add(fa); box.add(en); box.add(apply);
        p.add(box, BorderLayout.CENTER);
        return p;
    }

    // Phase 3: software 3D preview. Renders at the configured render scale
    // into a reused off-screen buffer; FPS is capped and the PERFORMANCE
    // profile is fully static, so weak systems never spin.
    private void openRendererPreview(final String worldName) {
        final JDialog dlg = new JDialog(this, "Gen5Craft - Play Session: " + worldName, false);
        dlg.setSize(900, 620);
        dlg.setLocationRelativeTo(this);
        final SoftwareRenderer renderer = new SoftwareRenderer();
        final Settings.Quality q = settings.effectiveQuality();
        final PerfProfile perf = PerfProfile.from(settings);
        final int scalePct = perf.effectiveScale(settings);
        final CameraState camera = new CameraState();
        final JPanel canvas = new JPanel() {
            private BufferedImage buf;
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = Math.max(160, getWidth() * scalePct / 100);
                int h = Math.max(120, getHeight() * scalePct / 100);
                if (buf == null || buf.getWidth() != w || buf.getHeight() != h) buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                renderer.render(buf, camera.yaw, camera.zoom);
                g.drawImage(buf, 0, 0, getWidth(), getHeight(), null);
                g.setColor(new Color(255, 255, 255, 190));
                g.drawString("World: " + worldName + " [" + Gen5Versions.latest() + "] | GPU: " + settings.gpuMode + " | Quality: " + q + " | Scale: " + scalePct + "%", 12, 20);
                g.drawString("Controls: A/D or drag = rotate, W/S or wheel = zoom", 12, 38);
            }
        };
        dlg.setContentPane(canvas);
        canvas.setFocusable(true);
        canvas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                char k = Character.toUpperCase(e.getKeyChar());
                if (k == 'A' || k == 'D' || k == 'W' || k == 'S') { camera.key(k); canvas.repaint(); }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_LEFT) { camera.rotate(-0.12); canvas.repaint(); }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_RIGHT) { camera.rotate(0.12); canvas.repaint(); }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) { camera.zoomBy(1.08); canvas.repaint(); }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) { camera.zoomBy(1.0 / 1.08); canvas.repaint(); }
            }
        });
        final int[] lastX = {-1};
        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { lastX[0] = e.getX(); canvas.requestFocusInWindow(); }
        });
        canvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (lastX[0] >= 0) { camera.rotate((e.getX() - lastX[0]) * 0.01); lastX[0] = e.getX(); canvas.repaint(); }
            }
        });
        canvas.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                camera.zoomBy(e.getWheelRotation() < 0 ? 1.1 : 1.0 / 1.1); canvas.repaint();
            }
        });
        if (!perf.staticScene && settings.panoramaAnimation) {
            int fps = Math.min(perf.targetFps, 30);
            final Timer timer = new Timer(Math.max(33, 1000 / Math.max(1, fps)), new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    camera.rotate(0.02);
                    canvas.repaint();
                }
            });
            timer.start();
            dlg.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosed(java.awt.event.WindowEvent e) { timer.stop(); }
            });
        }
        dlg.setVisible(true);
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(this, i18n.t("exitConfirm"), "Gen5Craft", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) System.exit(0);
    }
    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, String.valueOf(ex), "Gen5Craft", JOptionPane.ERROR_MESSAGE);
    }

    /** Dark glass panel drawn over the panorama. */
    private static final class GlassPanel extends JPanel {
        GlassPanel() { setOpaque(false); setBorder(new EmptyBorder(6, 6, 6, 6)); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(8, 12, 18, 178));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Pre-rendered panorama: rendered once per size, repainted from cache.
     *  Animation (when enabled) only shifts a cached layer with a capped timer;
     *  on the PERFORMANCE profile it is fully static. No painting allocations. */
    private static final class PanoramaPanel extends JPanel {
        private final Settings settings;
        private BufferedImage base;
        private int baseW, baseH, offset;
        private Timer timer;
        PanoramaPanel(Settings s) {
            settings = s;
            boolean animate = s.panoramaAnimation && s.effectiveQuality() != Settings.Quality.PERFORMANCE;
            if (animate) {
                int delay = s.fpsLimit > 0 ? Math.max(16, 1000 / Math.min(s.fpsLimit, 30)) : 33;
                timer = new Timer(delay, new ActionListener() {
                    public void actionPerformed(ActionEvent e) { offset = (offset + 1) % 240; repaint(); }
                });
                timer.start();
            }
        }
        private void ensureBase() {
            int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
            if (base != null && w == baseW && h == baseH) return;
            baseW = w; baseH = h;
            base = new BufferedImage(w + 240, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = base.createGraphics();
            GradientPaint sky = new GradientPaint(0, 0, new Color(24, 42, 74), 0, h, new Color(196, 120, 62));
            g.setPaint(sky); g.fillRect(0, 0, w + 240, h);
            g.setColor(new Color(255, 214, 130, 200));
            g.fillOval(w / 3, h / 6, 90, 90);
            long seed = 1234567L;
            for (int layer = 0; layer < 3; layer++) {
                int horizon = h * (55 + layer * 12) / 100;
                g.setColor(new Color(18 + layer * 10, 34 + layer * 12, 30 + layer * 8, 190));
                int x = 0;
                while (x < w + 240) {
                    seed = seed * 1103515245L + 12345L;
                    int bw = 40 + (int) ((seed >> 16) % 70);
                    int bh = 24 + (int) ((seed >> 20) % 70);
                    g.fillRect(x, horizon - bh, bw, bh + (h - horizon));
                    x += bw + 6;
                }
            }
            g.dispose();
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ensureBase();
            if (base != null) g.drawImage(base, -offset, 0, this);
        }
    }
}
