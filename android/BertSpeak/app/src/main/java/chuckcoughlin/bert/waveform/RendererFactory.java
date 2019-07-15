/**
 * @See: Mark Allison @ https://github.com/StylingAndroid/Visualizer
 */
package chuckcoughlin.bert.waveform;
import android.support.annotation.ColorInt;

import chuckcoughlin.bert.waveform.SimpleWaveformRenderer;
import chuckcoughlin.bert.waveform.WaveformRenderer;

public class RendererFactory {
    public WaveformRenderer createSimpleWaveformRenderer(@ColorInt int foreground, @ColorInt int background) {
        return SimpleWaveformRenderer.newInstance(background, foreground);
    }
}