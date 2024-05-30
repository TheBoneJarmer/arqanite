package be.labruyere.arqanite;

import be.labruyere.arqanore.Sound;

/**
 * In contrary to the Audio class, ArqMusic acts like a music player. And instead of directly playing the next song, you pick a song to play next.
 * Than if one was playing, it is being fade out, stopped, and the next song is being started and faded in.
 */
public class ArqMusic {
    static {
        maxVolume = 1.0;
    }

    private static Sound nextSong;
    private static Sound currentSong;

    private static double maxVolume;
    private static int step;
    private static int timer;
    private static boolean stop;
    private static boolean pause;

    public static double getMaxVolume() {
        return maxVolume;
    }

    public static void setMaxVolume(double value) {
        if (value > 1) {
            value = 1;
        }

        if (value < 0) {
            value = 0;
        }

        maxVolume = value;
    }

    public static void update() {
        updateNextSong();
        updateCurrentSong();
    }

    private static void updateCurrentSong() {
        if (currentSong == null) {
            return;
        }

        if (nextSong != null) {
            return;
        }

        if (stop) {
            var volume = currentSong.getVolume();

            if (volume > 0) {
                volume -= 0.1;
                currentSong.setVolume(volume);
            } else {
                currentSong.stop();
                currentSong = null;
                stop = false;
            }
        } else if (pause) {
            var volume = currentSong.getVolume();

            if (volume > 0) {
                volume -= 0.1;
                currentSong.setVolume(volume);
            } else {
                currentSong.setPaused(true);
            }
        } else {
            var paused = currentSong.isPaused();
            var volume = currentSong.getVolume();

            if (volume < maxVolume && !paused) {
                volume += 0.1;
                currentSong.setVolume(volume);
            }
        }
    }

    private static void updateNextSong() {
        if (nextSong == null) {
            return;
        }

        // Small exception to the flow is when no music was playing.
        // In that case we can skip step 0 and go to step 1 right away.
        if (step == 0 && currentSong == null) {
            step = 1;
        }

        // Fade out the current song
        if (step == 0) {
            var volume = currentSong.getVolume();

            if (timer < 1) {
                timer++;
                return;
            }

            if (volume > 0) {
                volume -= 0.1;
                currentSong.setVolume(volume);
            } else {
                currentSong.stop();
                step = 1;
            }

            timer = 0;
            return;
        }

        // Start playing the new song on silent
        if (step == 1) {
            nextSong.setLooping(true);
            nextSong.setVolume(0);
            nextSong.play();

            step = 2;
            return;
        }

        // And gradually fade it in
        if (step == 2) {
            var volume = nextSong.getVolume();

            if (volume < maxVolume) {
                volume += 0.1;
                nextSong.setVolume(volume);
            } else {
                currentSong = nextSong;
                nextSong = null;
                step = 0;
            }
        }
    }

    public static void play(Sound sound) {
        if (nextSong != null) {
            return;
        }

        // Do not play music that is already being played
        if (currentSong != null && sound.getAddress() == currentSong.getAddress()) {
            return;
        }

        nextSong = sound;
    }

    public static void stop() {
        if (currentSong == null) {
            return;
        }

        stop = true;
    }
}
