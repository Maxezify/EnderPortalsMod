package com.maxezify.enderportals;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Chronomètre les étapes qui touchent au monde de l'Ender, et n'écrit dans le
 * journal que ce qui dépasse.
 *
 * <p>Le gel de la première ouverture a résisté à trois corrections, chacune
 * fondée sur une cause plausible et chacune ne l'ayant que raccourci. Le bac à
 * sable où ce mod est écrit n'atteint ni Mojang ni le maven NeoForge : il n'y a
 * aucun moyen d'y lancer le jeu, donc aucun moyen d'y mesurer quoi que ce soit.
 * Continuer à deviner reviendrait à corriger au hasard.</p>
 *
 * <p>Ces sondes déplacent la mesure là où le jeu tourne vraiment. Elles
 * n'écrivent qu'au-delà de {@value #THRESHOLD_MS} ms — en deçà, rien ne se voit
 * à l'écran et le journal n'a pas à s'en encombrer. Le coût hors seuil est un
 * {@code System.nanoTime()} et une soustraction.</p>
 *
 * <p>Une fois la cause connue et corrigée, ce fichier n'a plus de raison
 * d'être : il est écrit pour être retiré.</p>
 */
public final class EnderPortalsTiming {

    /** Seuil d'écriture, en millisecondes. Un tick en dure 50. */
    private static final long THRESHOLD_MS = 20L;

    private static final AtomicLong CHUNKS = new AtomicLong();
    private static final AtomicLong CHUNK_NANOS = new AtomicLong();

    /** Chronomètre une étape nommée et journalise si elle dépasse le seuil. */
    public static void measure(String label, Runnable work) {
        long start = System.nanoTime();
        try {
            work.run();
        } finally {
            report(label, System.nanoTime() - start);
        }
    }

    public static long start() {
        return System.nanoTime();
    }

    public static void report(String label, long nanos) {
        long millis = nanos / 1_000_000L;
        if (millis >= THRESHOLD_MS) {
            EnderPortalsMod.LOGGER.info("[chrono] {} : {} ms", label, millis);
        }
    }

    public static void since(String label, long start) {
        report(label, System.nanoTime() - start);
    }

    /**
     * Cumule le temps de génération d'un chunk. Le total est écrit tous les
     * 64 chunks : un chunk isolé ne dit rien, la moyenne dit tout — c'est elle
     * qui départage un générateur lent d'un fil bloqué ailleurs.
     */
    public static void chunkGenerated(long nanos) {
        long count = CHUNKS.incrementAndGet();
        long total = CHUNK_NANOS.addAndGet(nanos);
        if (count % 64L == 0L) {
            EnderPortalsMod.LOGGER.info(
                    "[chrono] génération : {} chunks, {} ms au total, {} ms en moyenne",
                    count, total / 1_000_000L, (total / count) / 1_000_000.0);
        }
    }

    private EnderPortalsTiming() {
    }
}
