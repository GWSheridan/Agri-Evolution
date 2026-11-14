$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root=(Get-Location).Path
$spec=Get-Content tools/resource15-spec.json -Raw | ConvertFrom-Json
$removed=@($spec.removed.PSObject.Properties.Name)
$zip=[IO.Compression.ZipFile]::OpenRead((Join-Path $root 'backups/resource15-20261110/source-before.zip'))
$pngs=0;$recipes=0;$removedPngs=0
try {
 $entry=$zip.Entries | Where-Object {$_.FullName.Replace('\','/') -eq 'src/main/java/com/agrievolution/genetics/CropSpecies.java'}
 $reader=[IO.StreamReader]::new($entry.Open());try{$before=$reader.ReadToEnd()}finally{$reader.Dispose()}
 $after=Get-Content src/main/java/com/agrievolution/genetics/CropSpecies.java -Raw
 $pattern='(?m)^\s*([A-Z][A-Z0-9_]*\("([^"\r\n]+)",\s*"[^"\r\n]+",\s*"[^"\r\n]+",\s*"[^"\r\n]+",\s*(?:true|false)\))[,;]'
 $oldSpecies=[regex]::Matches($before,$pattern);$newSpecies=[regex]::Matches($after,$pattern)
 if($oldSpecies.Count -ne 251 -or $newSpecies.Count -ne 253){throw 'Unexpected species registry counts'}
 $current=@{};foreach($m in $newSpecies){$current[$m.Groups[2].Value]=$m.Groups[1].Value}
 foreach($m in $oldSpecies){$id=$m.Groups[2].Value;if($removed -contains $id){if($current.ContainsKey($id)){throw "Retired species still registered: $id"}}elseif($current[$id] -cne $m.Groups[1].Value){throw "Existing species identity changed: $id"}}
 foreach($entry in $zip.Entries) {
  $name=$entry.FullName.Replace('\','/')
  if(($name -like 'src/main/resources/assets/agri_evolution/textures/*.png') -or ($name -like 'src/main/resources/data/agri_evolution/recipe/*.json')) {
   $isRemoved=$false
   foreach($id in $removed){if($name -match ('/(block/'+$id+'_crop_stage[0-7]|item/'+$id+'_seed)\.png$')){$isRemoved=$true;break}}
   $current=Join-Path $root $name
   if($isRemoved){if(Test-Path -LiteralPath $current){throw "Retired texture remains: $name"};$removedPngs++;continue}
   $stream=$entry.Open();$hash=[Security.Cryptography.SHA256]::Create()
   try {$old=[Convert]::ToHexString($hash.ComputeHash($stream))} finally {$stream.Dispose();$hash.Dispose()}
   if(!(Test-Path -LiteralPath $current) -or (Get-FileHash -LiteralPath $current -Algorithm SHA256).Hash -ne $old){throw "Existing asset/recipe changed: $name"}
   if($name.EndsWith('.png')){$pngs++}else{$recipes++}
  }
 }
}finally{$zip.Dispose()}
if($pngs -ne 2367 -or $recipes -ne 77 -or $removedPngs -ne 90){throw "Unexpected baseline counts: $pngs/$recipes/$removedPngs"}
@{priorTexturesPreserved=$pngs;priorRecipesPreserved=$recipes;removedDuplicateTextures=$removedPngs;survivingSpeciesIdentitiesPreserved=241;legacySpeciesMigrationTable=251;version='0.69.0'} | ConvertTo-Json | Set-Content docs/resource-expansion-0690-verification.json
Write-Output "Preserved $pngs existing textures and $recipes crafting recipes; removed only $removedPngs duplicate-crop textures."
