/**
 * @See: Mark Allison @ https://github.com/StylingAndroid/Visualizer
 */
package chuckcoughlin.bert.waveform;

import android.graphics.Canvas;

public interface WaveformRenderer {
    void render(Canvas canvas, byte[] waveform);
}
