// Generates Track's launcher icons and splash logo from branding/trackit-logo.png.
// Run from the project root:  swift branding/make_icons.swift
import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let res = "app/src/main/res"
guard let src = CGImageSourceCreateWithURL(URL(fileURLWithPath: "branding/trackit-logo.png") as CFURL, nil),
      let logo = CGImageSourceCreateImageAtIndex(src, 0, nil) else { fatalError("cannot read logo") }

// Decode to RGBA8 so pixels can be read directly.
let w = logo.width, h = logo.height
var px = [UInt8](repeating: 0, count: w * h * 4)
let rgb = CGColorSpaceCreateDeviceRGB()
let readCtx = CGContext(data: &px, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4, space: rgb,
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
readCtx.draw(logo, in: CGRect(x: 0, y: 0, width: w, height: h))

func pixel(_ x: Int, _ y: Int) -> (Double, Double, Double) {
    let i = (y * w + x) * 4
    return (Double(px[i]), Double(px[i + 1]), Double(px[i + 2]))
}
func dist(_ a: (Double, Double, Double), _ b: (Double, Double, Double)) -> Double {
    ((a.0 - b.0) * (a.0 - b.0) + (a.1 - b.1) * (a.1 - b.1) + (a.2 - b.2) * (a.2 - b.2)).squareRoot()
}

// Background = corner colour; foreground = the pixel farthest from it (the cream text).
let bg = pixel(4, 4)
var fg = bg, best = 0.0
for y in stride(from: 0, to: h, by: 3) { for x in stride(from: 0, to: w, by: 3) {
    let d = dist(pixel(x, y), bg); if d > best { best = d; fg = pixel(x, y) }
} }
let full = dist(fg, bg)

// Wordmark as a transparent mask (anti-aliased edges become partial alpha) + its bounding box.
var mask = [UInt8](repeating: 0, count: w * h * 4)
var minX = w, minY = h, maxX = 0, maxY = 0
for y in 0..<h { for x in 0..<w {
    let a = min(1, max(0, dist(pixel(x, y), bg) / full))
    if a > 0.05 { minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y) }
    let i = (y * w + x) * 4
    mask[i] = UInt8(fg.0 * a); mask[i + 1] = UInt8(fg.1 * a); mask[i + 2] = UInt8(fg.2 * a); mask[i + 3] = UInt8(255 * a)
} }
let maskCtx = CGContext(data: &mask, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4, space: rgb,
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
// Bitmap rows are top-down; CG crop rects are top-down too for images.
let wordmark = maskCtx.makeImage()!.cropping(to: CGRect(x: minX, y: minY, width: maxX - minX + 1, height: maxY - minY + 1))!

func hex(_ c: (Double, Double, Double)) -> String { String(format: "#%02X%02X%02X", Int(c.0), Int(c.1), Int(c.2)) }
print("background \(hex(bg))  text \(hex(fg))  wordmark \(wordmark.width)x\(wordmark.height)")

/// Renders a square PNG: optional background, the wordmark centred at [widthFraction] of the side.
func render(size: Int, widthFraction: Double, background: (Double, Double, Double)?, round: Bool, to path: String) {
    let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: rgb,
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    ctx.interpolationQuality = .high
    let s = Double(size)
    if let b = background {
        if round { ctx.addEllipse(in: CGRect(x: 0, y: 0, width: s, height: s)); ctx.clip() }
        ctx.setFillColor(CGColor(red: b.0 / 255, green: b.1 / 255, blue: b.2 / 255, alpha: 1))
        ctx.fill(CGRect(x: 0, y: 0, width: s, height: s))
    }
    let tw = s * widthFraction
    let th = tw * Double(wordmark.height) / Double(wordmark.width)
    ctx.draw(wordmark, in: CGRect(x: (s - tw) / 2, y: (s - th) / 2, width: tw, height: th))
    let url = URL(fileURLWithPath: path)
    try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
    let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, ctx.makeImage()!, nil)
    CGImageDestinationFinalize(dest)
}

let densities: [(String, Double)] = [("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4)]
for (d, scale) in densities {
    // Adaptive foreground (108dp). Wordmark spans 56% so it stays inside the 66dp safe zone on any mask.
    render(size: Int(108 * scale), widthFraction: 0.56, background: nil, round: false,
           to: "\(res)/mipmap-\(d)/ic_launcher_foreground.png")
    // Legacy icons (48dp) for Android 7.x, which has no adaptive icons.
    render(size: Int(48 * scale), widthFraction: 0.74, background: bg, round: false, to: "\(res)/mipmap-\(d)/ic_launcher.png")
    render(size: Int(48 * scale), widthFraction: 0.70, background: bg, round: true, to: "\(res)/mipmap-\(d)/ic_launcher_round.png")
    // Splash icon (288dp canvas, shown inside a 192dp circle): wordmark spans 58% of the canvas.
    render(size: Int(288 * scale), widthFraction: 0.58, background: nil, round: false,
           to: "\(res)/drawable-\(d)/splash_logo.png")
}
print("brand_background=\(hex(bg))")
