package com.ariadne.wasm

import com.ariadne.app.ui.pages.PhotoPicker
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The library, in the browser.
 *
 * The demo had one way to put a receipt in: the shutter, which on a browser with no
 * camera behind it keeps a ruled plate. That is honest for a judge who has nothing to
 * hand, and useless for everyone else — a person reading the demo on the train has the
 * photograph already, in Files or in Photos, and no way to give it to the app.
 *
 * A browser's door for that is an `<input type="file">`. It is the same door as the
 * system photo picker on Android and the file dialog on desktop: one question ("which
 * image?"), one answer (bytes), and no storage permission anywhere. The chosen files
 * are read as data URLs, scaled to the same 1600px bound the Android picker uses, and
 * handed back through the same [PhotoPicker] the other targets implement — so the page
 * above does not know, and does not need to know, that this one is a web page.
 *
 * The read is asynchronous by nature (a person has to choose a file), which is why
 * [pickMany] suspends: the coroutine resumes from the change event, and a dialog
 * dismissed without a choice resumes with nothing at all.
 */
internal object BrowserPhotoPicker : PhotoPicker {

    override suspend fun pick(): ByteArray? = pickMany(limit = 1).firstOrNull()

    override suspend fun pickMany(limit: Int): List<ByteArray> =
        suspendCancellableCoroutine { continuation ->
            openFileChooser(limit.coerceIn(1, MOST_AT_ONCE)) { payload ->
                if (continuation.isActive) {
                    continuation.resume(decodeChosen(payload))
                }
            }
        }

    /** Twelve, the same bound as the Android picker: one evening's receipts. */
    private const val MOST_AT_ONCE = 12
}

/**
 * Opens the browser's file dialog and, when the person has chosen, hands back the
 * images as `data:` URLs joined by newlines. An empty string means "chose nothing".
 *
 * The scaling happens here rather than in Kotlin because the browser already has the
 * decoder and the canvas. A 12-megapixel photograph becomes a 1600px JPEG at the same
 * quality the phone's picker uses; if anything about that fails — an exotic codec, an
 * old engine, a canvas the browser refuses to give us — the original bytes are used
 * instead. Losing the rescale is acceptable; losing the photograph is not.
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun(
    """(limit, onDone) => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.multiple = limit > 1;
  input.style.position = 'fixed';
  input.style.left = '-10000px';
  input.style.top = '0';
  document.body.appendChild(input);

  let settled = false;
  const finish = (text) => {
    if (settled) return;
    settled = true;
    try { input.remove(); } catch (ignored) {}
    onDone(text);
  };

  const readOne = (file) => new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => {
      const original = typeof reader.result === 'string' ? reader.result : '';
      if (!original) { resolve(''); return; }
      const image = new Image();
      image.onload = () => {
        try {
          const longest = Math.max(image.width, image.height);
          if (!longest || longest <= 1600) { resolve(original); return; }
          const scale = 1600 / longest;
          const canvas = document.createElement('canvas');
          canvas.width = Math.round(image.width * scale);
          canvas.height = Math.round(image.height * scale);
          const context = canvas.getContext('2d');
          if (!context) { resolve(original); return; }
          context.drawImage(image, 0, 0, canvas.width, canvas.height);
          resolve(canvas.toDataURL('image/jpeg', 0.85));
        } catch (ignored) {
          resolve(original);
        }
      };
      image.onerror = () => resolve(original);
      image.src = original;
    };
    reader.onerror = () => resolve('');
    reader.readAsDataURL(file);
  });

  input.addEventListener('change', () => {
    const files = Array.prototype.slice.call(input.files || [], 0, limit);
    if (files.length === 0) { finish(''); return; }
    Promise.all(files.map(readOne)).then((urls) => {
      finish(urls.filter((url) => typeof url === 'string' && url).join('\n'));
    });
  });

  // Newer engines fire this when the dialog is dismissed without a choice; older ones
  // fire nothing at all, and the coroutine above simply never resumes.
  input.addEventListener('cancel', () => finish(''));

  input.click();
}""",
)
private external fun openFileChooser(limit: Int, onDone: (String) -> Unit)

/**
 * `data:image/jpeg;base64,…` lines into bytes.
 *
 * Anything that is not an image data URL is dropped rather than guessed at: a file
 * that is not a photograph has no place in a record of photographs, and a PDF read as
 * if it were an image would fail later, somewhere much less useful.
 */
private fun decodeChosen(payload: String): List<ByteArray> {
    if (payload.isEmpty()) return emptyList()
    return payload.split('\n').mapNotNull { line ->
        val comma = line.indexOf(',')
        if (comma <= 0) return@mapNotNull null
        val header = line.substring(0, comma)
        if (!header.startsWith("data:image/")) return@mapNotNull null
        if (!header.endsWith(";base64")) return@mapNotNull null
        decodeBase64(line.substring(comma + 1))?.takeIf { it.isNotEmpty() }
    }
}

/** Standard base64, tolerating the line wrapping some engines insert. */
private fun decodeBase64(text: String): ByteArray? {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val out = ArrayList<Byte>(text.length * 3 / 4 + 3)
    var buffer = 0
    var bits = 0
    for (character in text) {
        if (character == '=') break
        if (character == '\n' || character == '\r' || character == ' ') continue
        val value = alphabet.indexOf(character)
        if (value < 0) return null
        buffer = (buffer shl 6) or value
        bits += 6
        if (bits >= 8) {
            bits -= 8
            out.add(((buffer shr bits) and 0xFF).toByte())
            // Drop the byte that was just emitted, so the accumulator never grows
            // past the two digits still in flight.
            buffer = buffer and ((1 shl bits) - 1)
        }
    }
    return if (out.isEmpty()) null else out.toByteArray()
}
