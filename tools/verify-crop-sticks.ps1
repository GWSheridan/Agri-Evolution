$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root=(Get-Location).Path
$zip=[IO.Compression.ZipFile]::OpenRead((Join-Path $root 'backups/crop-sticks-20261112/source-before.zip'))
$changed=@('CropSupportItem.java','AgriCropBlock.java','AECropBlockEntity.java','BotanicalGreenhouseBlockEntity.java','MachineGuidePages.java')
$textures=0;$recipes=0;$java=0
try {
 foreach($entry in $zip.Entries){
  $name=$entry.FullName.Replace('\','/')
  if($name.EndsWith('/')){continue}
  if($name -match 'models/(block|item)/(iron_|obsidian_)?crop_support(_overlay)?\.json$'){continue}
  $resource=$name.StartsWith('src/main/resources/')
  $source=$name.StartsWith('src/main/java/') -and ($changed -notcontains [IO.Path]::GetFileName($name))
  if(!$resource -and !$source){continue}
  $stream=$entry.Open();$hash=[Security.Cryptography.SHA256]::Create()
  try{$old=[Convert]::ToHexString($hash.ComputeHash($stream))}finally{$stream.Dispose();$hash.Dispose()}
  $current=Join-Path $root $name
  if(!(Test-Path -LiteralPath $current) -or (Get-FileHash -LiteralPath $current -Algorithm SHA256).Hash -ne $old){throw "Unrelated source/resource changed: $name"}
  if($name.EndsWith('.png')){$textures++}
  if($name.StartsWith('src/main/resources/data/agri_evolution/recipe/')){$recipes++}
  if($source){$java++}
 }
}finally{$zip.Dispose()}
if($textures -ne 2583 -or $recipes -ne 77){throw 'Unexpected preserved counts'}
@{version='0.75.0';preservedTextures=$textures;preservedRecipes=$recipes;unchangedJavaFiles=$java;gameTests=126;jarSHA256=(Get-FileHash build/libs/agri_evolution-0.75.0.jar -Algorithm SHA256).Hash} | ConvertTo-Json | Set-Content docs/crop-sticks-0750-verification.json
Write-Output "Preserved all $textures textures, $recipes recipes and $java unrelated Java files."
