$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root=(Get-Location).Path
$zip=[IO.Compression.ZipFile]::OpenRead((Join-Path $root 'backups/farmland-rework-20261110/source-before.zip'))
$changed=@();$preserved=0;$textures=0;$recipes=0;$java=0
try {
 foreach($entry in $zip.Entries){
  $name=$entry.FullName.Replace('\','/')
  if(!$name.StartsWith('src/main/') -or $name.EndsWith('/')){continue}
  $stream=$entry.Open();$hash=[Security.Cryptography.SHA256]::Create()
  try{$old=[Convert]::ToHexString($hash.ComputeHash($stream))}finally{$stream.Dispose();$hash.Dispose()}
  $path=Join-Path $root $name
  if(!(Test-Path -LiteralPath $path)){throw "Deleted source: $name"}
  if((Get-FileHash -LiteralPath $path).Hash -ne $old){
   if($name -notmatch '^src/main/resources/assets/agri_evolution/(textures/block/(cultivated|nutrient|mythic|arcane|celestial)_farmland_(dry|wet|side)\.png|models/block/(cultivated|nutrient|mythic|arcane|celestial)_farmland_(dry|wet)\.json)$'){throw "Unexpected change: $name"}
   $changed+=$name
  }else{
   $preserved++
   if($name.EndsWith('.png')){$textures++}
   if($name -like 'src/main/resources/data/agri_evolution/recipe/*.json'){$recipes++}
   if($name.EndsWith('.java')){$java++}
  }
 }
}finally{$zip.Dispose()}
if($changed.Count -ne 25 -or $textures -ne 2334 -or $recipes -ne 77){throw 'Unexpected change scope'}
@{version='0.67.1';changed=$changed;unchangedMainFiles=$preserved;unchangedTextures=$textures;unchangedCraftingRecipes=$recipes;unchangedGameplayJavaFiles=$java} | ConvertTo-Json -Depth 5 | Set-Content docs/farmland-0671-verification.json
Write-Output "Only 15 farmland textures and 10 farmland models changed. Preserved $textures other PNGs, $recipes recipes and $java Java sources."
