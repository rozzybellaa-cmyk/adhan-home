package com.adhan.home;

import java.util.Calendar;
import java.util.TimeZone;

/** Prayer times for Riyadh (Umm al-Qura style: Fajr 18.5 deg, Isha 90 min after Maghrib, 120 in Ramadan). */
public class PrayerTimes {
    public static final double LAT = 24.7136, LON = 46.6753;
    static final int TZ = 3;
    static final double R = Math.PI / 180;
    public static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

    public static class Next {
        public final int idx;
        public final long time;
        Next(int idx, long time) { this.idx = idx; this.time = time; }
    }

    static double fix(double a, double b) {
        a = a - b * Math.floor(a / b);
        return a < 0 ? a + b : a;
    }

    static long utc(int y, int m, int d, int hh) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(y, m, d, hh, 0, 0);
        return c.getTimeInMillis();
    }

    static boolean isRamadan(long ms) {
        try {
            android.icu.util.IslamicCalendar ic = new android.icu.util.IslamicCalendar();
            ic.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            ic.setTimeInMillis(ms);
            return ic.get(android.icu.util.Calendar.MONTH) == android.icu.util.IslamicCalendar.RAMADAN;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Six times (fajr, sunrise, dhuhr, asr, maghrib, isha) in millis for the Riyadh day containing now + offset days. */
    public static long[] forDay(long now, int offset) {
        long shifted = now + TZ * 3600000L + offset * 86400000L;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(shifted);
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);
        boolean ramadan = isRamadan(now + offset * 86400000L);

        double jd = utc(y, m, d, 12) / 86400000.0 + 2440587.5 - TZ / 24.0;
        double D = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * D, 360);
        double q = fix(280.459 + 0.98564736 * D, 360);
        double L = fix(q + 1.915 * Math.sin(g * R) + 0.020 * Math.sin(2 * g * R), 360);
        double e = 23.439 - 0.00000036 * D;
        double ra = Math.atan2(Math.cos(e * R) * Math.sin(L * R), Math.cos(L * R)) / R / 15;
        double eqt = q / 15 - fix(ra, 24);
        if (eqt > 12) eqt -= 24;
        if (eqt < -12) eqt += 24;
        double dec = Math.asin(Math.sin(e * R) * Math.sin(L * R)) / R;
        double noon = 12 - eqt - LON / 15 + TZ;

        double asrAngle = -Math.atan(1 / (1 + Math.tan(Math.abs(LAT - dec) * R))) / R;
        double sunset = noon + t(0.833, dec);
        double[] h = {
            noon - t(18.5, dec),
            noon - t(0.833, dec),
            noon + 0.02,
            noon + t(asrAngle, dec),
            sunset,
            sunset + (ramadan ? 120 : 90) / 60.0
        };
        long base = utc(y, m, d, 0);
        long[] out = new long[6];
        for (int i = 0; i < 6; i++) out[i] = base + (long) ((h[i] - TZ) * 3600000.0);
        return out;
    }

    static double t(double angle, double dec) {
        double c = (-Math.sin(angle * R) - Math.sin(LAT * R) * Math.sin(dec * R)) / (Math.cos(LAT * R) * Math.cos(dec * R));
        return Math.acos(c) / R / 15;
    }

    /** Next prayer that has an adhan (sunrise excluded). */
    public static Next next(long now) {
        long[] t = forDay(now, 0);
        int[] adh = {0, 2, 3, 4, 5};
        for (int i : adh) if (t[i] > now) return new Next(i, t[i]);
        long[] t2 = forDay(now, 1);
        return new Next(0, t2[0]);
    }
}
