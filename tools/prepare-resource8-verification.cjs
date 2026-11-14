const fs=require('fs');
let s=fs.readFileSync('tools/verify-resource7.cjs','utf8')
 .replaceAll('resource7-spec','resource8-spec')
 .replaceAll('resources7-','resources8-')
 .replaceAll('0550','0560').replaceAll('0.55.0','0.56.0')
 .replaceAll('1048','1144').replaceAll('1296','1404').replaceAll('1188','1296')
 .replaceAll('131','143').replaceAll('resourceCrops:97','resourceCrops:109')
 .replaceAll('80 required','83 required').replaceAll('Passed:80','Passed:83').replaceAll('80 GameTests','83 GameTests')
 .replaceAll('Recipes:128','Recipes:140').replaceAll('Recipes:84','Recipes:92').replaceAll('Recipes:42','Recipes:46');
fs.writeFileSync('tools/verify-resource8.cjs',s);
fs.copyFileSync('artwork/resource-expansion-v7/journal-contact-sheet.ps1','artwork/resource-expansion-v8/journal-contact-sheet.ps1');

