$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = 'src/main/resources/assets/agri_evolution/textures'
$variants = [ordered]@{ wood = 'crop_support'; iron = 'iron_crop_support'; obsidian = 'obsidian_crop_support' }

function Get-OpaqueBounds($image, [int]$left, [int]$right) {
    $minX = $right; $minY = $image.Height; $maxX = -1; $maxY = -1
    for ($y = 0; $y -lt $image.Height; $y++) {
        for ($x = $left; $x -lt $right; $x++) {
            if ($image.GetPixel($x, $y).A -lt 160) { continue }
            $minX = [Math]::Min($minX, $x); $maxX = [Math]::Max($maxX, $x)
            $minY = [Math]::Min($minY, $y); $maxY = [Math]::Max($maxY, $y)
        }
    }
    if ($maxX -lt $minX) { throw 'No opaque content in source region' }
    [Drawing.Rectangle]::new($minX, $minY, $maxX - $minX + 1, $maxY - $minY + 1)
}

function Copy-ScaledPixels($source, $bounds, $output, [int]$left, [int]$top, [int]$width, [int]$height, [bool]$opaque) {
    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            $sx = $bounds.X + [int][Math]::Floor(($x + 0.5) * $bounds.Width / $width)
            $sy = $bounds.Y + [int][Math]::Floor(($y + 0.5) * $bounds.Height / $height)
            $pixel = $source.GetPixel($sx, $sy)
            if ($opaque -and $pixel.A -lt 250) { throw "Material sample crosses the transparent edge at $sx,$sy" }
            if ($opaque -or $pixel.A -ge 160) {
                $output.SetPixel($left + $x, $top + $y, [Drawing.Color]::FromArgb(255, $pixel.R, $pixel.G, $pixel.B))
            }
        }
    }
}

foreach ($variant in $variants.GetEnumerator()) {
    $masterPath = Join-Path (Get-Location) "artwork/crop-stick-art/masters/$($variant.Key).png"
    $source = [Drawing.Bitmap]::new($masterPath)
    $icon = [Drawing.Bitmap]::new(64, 64)
    $material = [Drawing.Bitmap]::new(64, 64)
    try {
        $split = [int]($source.Width * 0.64)
        $iconBounds = Get-OpaqueBounds $source 0 $split
        $scale = 58.0 / [Math]::Max($iconBounds.Width, $iconBounds.Height)
        $width = [int]($iconBounds.Width * $scale)
        $height = [int]($iconBounds.Height * $scale)
        Copy-ScaledPixels $source $iconBounds $icon ([int][Math]::Floor((64-$width)/2)) ([int][Math]::Floor((64-$height)/2)) $width $height $false

        $shaft = Get-OpaqueBounds $source $split $source.Width
        $shaft.Inflate(-[int]($shaft.Width * 0.12), -[int]($shaft.Height * 0.06))
        Copy-ScaledPixels $source $shaft $material 0 0 64 64 $true

        $icon.Save((Join-Path (Get-Location) "$assetRoot/item/$($variant.Value).png"), [Drawing.Imaging.ImageFormat]::Png)
        $material.Save((Join-Path (Get-Location) "$assetRoot/block/$($variant.Key)_crop_stick_shaft.png"), [Drawing.Imaging.ImageFormat]::Png)
        Write-Output "$($variant.Key): icon $iconBounds | shaft $shaft"
    } finally {
        $source.Dispose(); $icon.Dispose(); $material.Dispose()
    }
}
