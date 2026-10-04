package dev.asrithtanniru.dotwall.render

import android.content.Context
import android.graphics.Typeface
import dev.asrithtanniru.dotwall.R

/** Typefaces for wallpaper text. Bundled Google Sans Flex (OFL), matching the Pixel system look. */
class WallpaperFonts(val medium: Typeface, val semibold: Typeface) {
    companion object {
        /** Stock Android fonts, used when no Context is at hand (e.g. tests). */
        val System = WallpaperFonts(
            Typeface.create("sans-serif-medium", Typeface.NORMAL),
            Typeface.create(Typeface.DEFAULT, 600, false),
        )

        fun load(context: Context): WallpaperFonts = runCatching {
            WallpaperFonts(
                context.resources.getFont(R.font.google_sans_medium),
                context.resources.getFont(R.font.google_sans_semibold),
            )
        }.getOrDefault(System)
    }
}
