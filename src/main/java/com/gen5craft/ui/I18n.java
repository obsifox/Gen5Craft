package com.gen5craft.ui;

import java.util.HashMap;
import java.util.Map;

/** Minimal bilingual strings. Persian/Farsi is the default (RTL header);
 *  English switches the layout to LTR. New keys fall back to the key itself. */
public final class I18n {
    private final Map<String, String> fa = new HashMap<String, String>();
    private final Map<String, String> en = new HashMap<String, String>();
    private String lang = "fa";
    public I18n() {
        put("language", "زبان", "Language");
        put("multiplayer", "مولتی‌پلیر", "Multiplayer");
        put("singleplayer", "سینگل‌پلیر", "Singleplayer");
        put("settings", "تنظیمات", "Settings");
        put("exit", "خروج", "Exit");
        put("gpuSettings", "تنظیمات GPU / گرافیک", "GPU / Graphics settings");
        put("gpuMode", "حالت گرافیک", "Graphics mode");
        put("gpuAuto", "خودکار (پیشنهادی)", "Auto (recommended)");
        put("gpuIntegrated", "گرافیک داخلی CPU", "Integrated (CPU graphics)");
        put("gpuDedicated", "کارت گرافیک مجزا", "Dedicated graphics card");
        put("gpuCpuSoftware", "رندر نرم‌افزاری با CPU (پشتیبان سیستم ضعیف)", "CPU software rendering (weak-system fallback)");
        put("quality", "پروفایل کارایی", "Performance profile");
        put("renderScale", "مقیاس رندر", "Render scale");
        put("fpsLimit", "سقف FPS", "FPS limit");
        put("panoramaAnim", "انیمیشن آرام پانوراما", "Slow panorama animation");
        put("save", "ذخیره", "Save");
        put("saved", "ذخیره شد.", "Saved.");
        put("serverAddress", "آدرس سرور", "Server address");
        put("serverVersion", "نسخهٔ سرور", "Server version");
        put("checkServer", "بررسی نسخه و ساخت پروفایل", "Check version & build profile");
        put("newWorld", "ساخت دنیای جدید (با آخرین دادهٔ 26.3)", "Create new world (latest 26.3 data)");
        put("downloadData", "دانلود داده‌های 26.3 از مانیفست رسمی", "Download 26.3 data from the official manifest");
        put("worldsEmpty", "هنوز دنیایی ساخته نشده است.", "No worlds yet.");
        put("exitConfirm", "از کلاینت خارج می‌شوید؟", "Exit the client?");
    }
    private void put(String k, String f, String e) { fa.put(k, f); en.put(k, e); }
    public void setLanguage(String l) { lang = "en".equals(l) ? "en" : "fa"; }
    public String language() { return lang; }
    public boolean rtl() { return "fa".equals(lang); }
    public String t(String k) {
        String v = ("fa".equals(lang) ? fa : en).get(k);
        return v == null ? k : v;
    }
}
