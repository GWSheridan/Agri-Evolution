$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root=(Get-Location).Path
$zip=[IO.Compression.ZipFile]::OpenRead((Join-Path $root 'backups/food-expansion6-20261110/source-before.zip'))
$pngs=0;$recipes=0
try {
 $speciesEntry=$zip.Entries | Where-Object {$_.FullName.Replace('\','/') -eq 'src/main/java/com/agrievolution/genetics/CropSpecies.java'}
 $reader=[IO.StreamReader]::new($speciesEntry.Open());try{$before=$reader.ReadToEnd()}finally{$reader.Dispose()}
 $after=Get-Content src/main/java/com/agrievolution/genetics/CropSpecies.java -Raw
 $pattern='(?m)^\s*([A-Z][A-Z0-9_]*\("[^"\r\n]+",\s*"[^"\r\n]+",\s*"[^"\r\n]+",\s*"[^"\r\n]+",\s*(?:true|false)\))[,;]'
 $oldSpecies=[regex]::Matches($before,$pattern);$newSpecies=[regex]::Matches($after,$pattern)
 if($oldSpecies.Count -ne 215 -or $newSpecies.Count -ne 227){throw 'Unexpected species registry counts'}
 for($i=0;$i -lt $oldSpecies.Count;$i++){if($oldSpecies[$i].Groups[1].Value -cne $newSpecies[$i].Groups[1].Value){throw "Saved species identity changed at ordinal $i"}}
 foreach($entry in $zip.Entries) {
  $name=$entry.FullName.Replace('\','/')
  if(($name -like 'src/main/resources/assets/agri_evolution/textures/*.png') -or ($name -like 'src/main/resources/data/agri_evolution/recipe/*.json')) {
   $stream=$entry.Open();$hash=[Security.Cryptography.SHA256]::Create()
   try {$old=[Convert]::ToHexString($hash.ComputeHash($stream))} finally {$stream.Dispose();$hash.Dispose()}
   $current=Join-Path $root $name
   if(!(Test-Path -LiteralPath $current) -or (Get-FileHash -LiteralPath $current -Algorithm SHA256).Hash -ne $old){throw "Existing asset/recipe changed: $name"}
   if($name.EndsWith('.png')){$pngs++}else{$recipes++}
  }
 }
}finally{$zip.Dispose()}
if($pngs -ne 2104 -or $recipes -ne 60){throw "Unexpected baseline counts: $pngs textures, $recipes recipes"}
@{priorTexturesPreserved=$pngs;priorRecipesPreserved=$recipes;priorSpeciesIdentitiesAndOrdinalsPreserved=215;version='0.64.0'} | ConvertTo-Json | Set-Content docs/food-expansion-0640-verification.json
Write-Output "Preserved all $pngs previous textures and $recipes previous crafting recipes."
