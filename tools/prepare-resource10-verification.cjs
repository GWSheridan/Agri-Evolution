const fs=require('fs');
let s=fs.readFileSync('tools/verify-resource9.cjs','utf8')
 .replaceAll('resource9-spec','resource10-spec').replaceAll('resources9-','resources10-')
 .replaceAll('0570','0580').replaceAll('0.57.0','0.58.0')
 .replaceAll('1240','1336').replaceAll('1512','1620').replaceAll('1404','1512')
 .replaceAll('155','167').replaceAll('resourceCrops:121','resourceCrops:133')
 .replaceAll('86 required','89 required').replaceAll('Passed:86','Passed:89').replaceAll('86 GameTests','89 GameTests')
 .replaceAll('Recipes:152','Recipes:164').replaceAll('Recipes:100','Recipes:108').replaceAll('Recipes:50','Recipes:54');
fs.writeFileSync('tools/verify-resource10.cjs',s);
fs.copyFileSync('artwork/resource-expansion-v9/journal-contact-sheet.ps1','artwork/resource-expansion-v10/journal-contact-sheet.ps1');
