const fs=require('fs');
let s=fs.readFileSync('tools/verify-resource10.cjs','utf8')
 .replaceAll('resource10-spec','resource11-spec').replaceAll('resources10-','resources11-')
 .replaceAll('0580','0590').replaceAll('0.58.0','0.59.0')
 .replaceAll('1336','1432').replaceAll('1620','1728').replaceAll('1512','1620')
 .replaceAll('167','179').replaceAll('resourceCrops:133','resourceCrops:145')
 .replaceAll('89 required','92 required').replaceAll('Passed:89','Passed:92').replaceAll('89 GameTests','92 GameTests')
 .replaceAll('Recipes:164','Recipes:176').replaceAll('Recipes:108','Recipes:116').replaceAll('Recipes:54','Recipes:58');
fs.writeFileSync('tools/verify-resource11.cjs',s);
fs.copyFileSync('artwork/resource-expansion-v10/journal-contact-sheet.ps1','artwork/resource-expansion-v11/journal-contact-sheet.ps1');
