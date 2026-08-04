package com.maxezify.enderportals;

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
 * <p>Elles ont répondu. La sonde du générateur — 8,4 ms par chunk en moyenne
 * sur 512 chunks, l'ordre de grandeur de vanilla — l'a innocenté et a été
 * retirée. Les quatre autres n'ont jamais rien écrit : chaque étape du mod
 * coûte moins de 20 ms. Le gel venait d'ailleurs, et la suite du journal l'a
 * nommé : Iris compilant sa passe de rendu pour une dimension qui apparaît
 * (voir le README).</p>
 *
 * <p>Ce qui reste ne coûte rien tant que rien ne traîne, et redeviendra utile
 * le jour où quelque chose traînera.</p>
 */
public final class EnderPortalsTiming {

    /** Seuil d'écriture, en millisecondes. Un tick en dure 50. */
    private static final long THRESHOLD_MS = 20L;

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

    private static void report(String label, long nanos) {
        long millis = nanos / 1_000_000L;
        if (millis >= THRESHOLD_MS) {
            EnderPortalsMod.LOGGER.info("[chrono] {} : {} ms", label, millis);
        }
    }

    public static void since(String label, long start) {
        report(label, System.nanoTime() - start);
    }

    private EnderPortalsTiming() {
    }
}
