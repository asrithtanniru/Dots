"""Reference renderer for the lock-screen dot wallpapers (mirrors the Kotlin drawing spec in PLAN.md).
Draws at 2x and downsamples for smooth dots. Output: 1080x2400 (Pixel 7a)."""
import math, datetime as dt
from PIL import Image, ImageDraw, ImageFont

W, H, SS = 1080, 2400, 2
FONT = "/usr/share/fonts/truetype/inter/Inter-Regular.ttf"
import glob
def ifont(weight, px):
    return ImageFont.truetype(f"/usr/share/fonts/opentype/inter/Inter-{weight}.otf", px)
def font(weight, size):
    return ifont(weight, size * SS)

BG, PAST, FUTURE, ACCENT, MUTED = "#0D0D0C", "#ECEAE4", "#2A2927", "#D97757", "#7C7A74"
# Safe band (fraction of height): below the clock + At a Glance, above the bottom shortcut row.
TOP, BOTTOM, SIDE = 0.375, 0.905, 0.085


def best_cols(n, w, h, lo=7, hi=60):
    best = None
    for c in range(lo, hi + 1):
        r = math.ceil(n / c)
        p = min(w / c, h / r)
        if best is None or p > best[0] + 1e-6:
            best = (p, c, r)
    return best


def dot_grid(d, x0, y0, w, h, n, filled, today, cols=None, max_pitch=70, header=None, footer=None, gap_label=56):
    """Grid of n dots centred in the box. filled = dots before today; today = index or -1."""
    # reserve room for header/footer text
    head_h = gap_label * SS if header else 0
    foot_h = gap_label * SS if footer else 0
    gw, gh = w, h - head_h - foot_h
    if cols:
        rows = math.ceil(n / cols); pitch = min(gw / cols, gh / rows)
    else:
        pitch, cols, rows = best_cols(n, gw, gh)
    pitch = min(pitch, max_pitch * SS)
    r = pitch * 0.36
    tw, th = cols * pitch, rows * pitch
    total_h = th + head_h + foot_h
    ox = x0 + (w - tw) / 2
    oy = y0 + (h - total_h) / 2 + head_h
    for i in range(n):
        cx = ox + (i % cols + 0.5) * pitch
        cy = oy + (i // cols + 0.5) * pitch
        col = ACCENT if i == today else (PAST if i < filled else FUTURE)
        rr = max(r * 1.35, r + 5 * SS) if i == today else r
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=col)
    cx = x0 + w / 2
    if header:
        d.text((cx, oy - 22 * SS), header, font=font("Medium", 36), fill="#BDBAB3", anchor="ms")
    if footer:
        left, right = footer
        f = font("Medium", 32)
        lw = d.textlength(left, font=f); sep = "  ·  "; sw = d.textlength(sep, font=f); rw = d.textlength(right, font=f)
        sx = cx - (lw + sw + rw) / 2; y = oy + th + 50 * SS
        d.text((sx, y), left, font=f, fill=ACCENT, anchor="ls")
        d.text((sx + lw, y), sep, font=f, fill=MUTED, anchor="ls")
        d.text((sx + lw + sw, y), right, font=f, fill=MUTED, anchor="ls")


def canvas():
    im = Image.new("RGB", (W * SS, H * SS), BG)
    return im, ImageDraw.Draw(im)


def band():
    return SIDE * W * SS, TOP * H * SS, (1 - 2 * SIDE) * W * SS, (BOTTOM - TOP) * H * SS


def finish(im, name):
    im = im.resize((W, H), Image.LANCZOS); im.save(name); return im


TODAY = dt.date(2026, 10, 4)


def year(name, today=TODAY):
    im, d = canvas()
    start = dt.date(today.year, 1, 1); n = (dt.date(today.year + 1, 1, 1) - start).days
    idx = (today - start).days
    left = n - idx - 1
    pct = round(100 * (idx + 1) / n)
    dot_grid(d, *band(), n, idx, idx, footer=(f"{left}d left", f"{pct}%"))
    return finish(im, name)


def year_months(name, today=TODAY):
    im, d = canvas()
    x0, y0, w, h = band()
    cols_m, rows_m = 3, 4
    label_h = 46 * SS; foot = 70 * SS
    cell_w = w / cols_m
    pitch = min((cell_w - 40 * SS) / 7, (h - foot) / rows_m / 7.6)
    r = pitch * 0.36
    block_h = 6 * pitch + label_h
    gap_y = ((h - foot) - rows_m * block_h) / (rows_m - 1 + 0.0001)
    gap_y = min(gap_y, 60 * SS)
    total = rows_m * block_h + (rows_m - 1) * gap_y
    oy = y0 + ((h - foot) - total) / 2
    f = font("SemiBold", 26)
    for m in range(12):
        bx = x0 + (m % 3) * cell_w + (cell_w - 7 * pitch) / 2
        by = oy + (m // 3) * (block_h + gap_y)
        first = dt.date(today.year, m + 1, 1)
        days = ((dt.date(today.year + (m == 11), (m + 1) % 12 + 1, 1)) - first).days
        offset = first.weekday()  # Monday-first
        d.text((bx + r * 0.2, by + 26 * SS), first.strftime("%b").upper(), font=f,
               fill=ACCENT if m + 1 == today.month else MUTED, anchor="ls")
        for i in range(days):
            k = i + offset
            cx = bx + (k % 7 + 0.5) * pitch; cy = by + label_h + (k // 7 + 0.5) * pitch
            day = dt.date(today.year, m + 1, i + 1)
            col = ACCENT if day == today else (PAST if day < today else FUTURE)
            d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
    start = dt.date(today.year, 1, 1); n = (dt.date(today.year + 1, 1, 1) - start).days; idx = (today - start).days
    ff = font("Medium", 32); cx = x0 + w / 2; y = oy + total + 64 * SS
    a, sep, b = f"{n-idx-1}d left", "  ·  ", f"{round(100*(idx+1)/n)}%"
    lw, sw, rw = (d.textlength(t, font=ff) for t in (a, sep, b)); sx = cx - (lw + sw + rw) / 2
    d.text((sx, y), a, font=ff, fill=ACCENT, anchor="ls"); d.text((sx + lw, y), sep, font=ff, fill=MUTED, anchor="ls")
    d.text((sx + lw + sw, y), b, font=ff, fill=MUTED, anchor="ls")
    return finish(im, name)


def life(name, birth=dt.date(2004, 3, 15), years=80, today=TODAY):
    im, d = canvas()
    n = years * 52
    lived = min(n, (today - birth).days // 7)
    left_y = years - (today - birth).days / 365.25
    dot_grid(d, *band(), n, lived, lived, cols=52, footer=(f"{left_y:.0f}y left", f"{round(100*lived/n)}% lived"))
    return finish(im, name)


def goal(name, title="Half marathon", start=dt.date(2026, 9, 1), end=dt.date(2026, 12, 31), today=TODAY):
    im, d = canvas()
    n = (end - start).days + 1; idx = (today - start).days
    dot_grid(d, *band(), n, idx, idx, cols=14, max_pitch=58, header=title,
             footer=(f"{(end-today).days}d left", f"{round(100*(idx+1)/n)}%"))
    return finish(im, name)


def lockscreen(src, name):
    """Overlay an approximation of the Pixel 7a lock screen to check placement."""
    im = Image.open(src).convert("RGBA"); ov = Image.new("RGBA", im.size, (0, 0, 0, 0)); d = ImageDraw.Draw(ov)
    d.text((88, 390), "08 13", font=ifont('Light', 230), fill="#EDEDED", anchor="ls")
    s = ifont('Medium', 46)
    d.text((86, 545), "Sun, Oct 4   ☾ 27°C", font=s, fill="#EDEDED", anchor="ls")
    d.text((86, 695), "32°C in Pune tomorrow", font=s, fill="#EDEDED", anchor="ls")
    d.text((86, 780), "2° colder than today", font=ifont('Medium', 40), fill="#CFCFCF", anchor="ls")
    d.ellipse([440, 1615, 640, 1815], fill=(40, 40, 40, 215))
    for cx in (105, 975):
        d.ellipse([cx - 62, 2190, cx + 62, 2314], fill=(40, 40, 40, 215))
    d.text((540, 2265), "Asrith", font=s, fill="#EDEDED", anchor="ms")
    d.rounded_rectangle([400, 2358, 680, 2366], 4, fill="#EDEDED")
    out = Image.alpha_composite(im, ov).convert("RGB"); out.save(name); return out


if __name__ == "__main__":
    year("wall-year.png"); year_months("wall-year-months.png"); life("wall-life.png"); goal("wall-goal.png")
    for k in ("year", "year-months", "life", "goal"):
        lockscreen(f"wall-{k}.png", f"lock-{k}.png")
    # side-by-side contact sheet of the lock-screen previews
    ims = [Image.open(f"lock-{k}.png").resize((540, 1200), Image.LANCZOS) for k in ("year", "year-months", "life", "goal")]
    sheet = Image.new("RGB", (540 * 4 + 60 * 5, 1200 + 120), "#161615")
    for i, x in enumerate(ims):
        sheet.paste(x, (60 + i * 600, 60))
    sheet.save("preview-all.png")
