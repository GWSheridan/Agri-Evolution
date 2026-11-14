$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$source = [Drawing.Bitmap]::new((Resolve-Path 'artwork/vertical-greenhouse-copper/copper-sheet.png').Path)
$textures = (Resolve-Path 'src/main/resources/assets/agri_evolution/textures/block').Path

function Get-SwatchBounds([int]$top, [int]$bottom) {
    $left = $source.Width; $right = -1; $first = $bottom; $last = -1
    for ($y = $top; $y -lt $bottom; $y++) {
        for ($x = 0; $x -lt $source.Width; $x++) {
            if ($source.GetPixel($x, $y).A -lt 250) { continue }
            $left = [Math]::Min($left, $x); $right = [Math]::Max($right, $x)
            $first = [Math]::Min($first, $y); $last = [Math]::Max($last, $y)
        }
    }
    if ($right -lt $left) { throw "Missing opaque swatch in rows $top to $bottom" }
    $bounds = [Drawing.Rectangle]::new($left, $first, $right-$left+1, $last-$first+1)
    $bounds.Inflate(-5, -4)
    return $bounds
}

function Save-Swatch($bounds, [string]$name, [int]$offset, [int]$height) {
    $tile = [Drawing.Bitmap]::new(64, 64)
    try {
        for ($y = 0; $y -lt 64; $y++) {
            # Extend the edge colours outside the mapped rail to protect its mip levels.
            $row = [Math]::Clamp($y - $offset, 0, $height - 1)
            $sy = $bounds.Y + [int][Math]::Floor(($row + 0.5) * $bounds.Height / $height)
            for ($x = 0; $x -lt 64; $x++) {
                $sx = $bounds.X + [int][Math]::Floor(($x + 0.5) * $bounds.Width / 64)
                $pixel = $source.GetPixel($sx, $sy)
                if ($pixel.A -lt 250) { throw "$name sampled a transparent edge at $sx,$sy" }
                $tile.SetPixel($x, $y, [Drawing.Color]::FromArgb(255, $pixel.R, $pixel.G, $pixel.B))
            }
        }
        $tile.Save((Join-Path $textures "$name.png"), [Drawing.Imaging.ImageFormat]::Png)
        Write-Output "$name sampled from $bounds"
    } finally { $tile.Dispose() }
}

try {
    # The transparent gap sits above the halfway line of the lower swatch.
    $split = [int]($source.Height * 0.43)
    Save-Swatch (Get-SwatchBounds 0 $split) 'vertical_greenhouse_copper_rail' 24 16
    Save-Swatch (Get-SwatchBounds $split $source.Height) 'vertical_greenhouse_copper_plate' 0 64
} finally { $source.Dispose() }
