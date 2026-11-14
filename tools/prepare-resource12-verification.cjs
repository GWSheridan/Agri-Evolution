const fs=require('fs');
let s=fs.readFileSync('tools/verify-resource11.cjs','utf8')
 .replaceAll('resource11-spec','resource12-spec').replaceAll('resources11-','resources12-')
 .replaceAll('0590','0600').replaceAll('0.59.0','0.60.0')
 .replaceAll('1432','1528').replaceAll('1728','1836').replaceAll('1620','1728')
 .replaceAll('179','191').replaceAll('resourceCrops:145','resourceCrops:157')
 .replaceAll('92 required','95 required').replaceAll('Passed:92','Passed:95').replaceAll('92 GameTests','95 GameTests')
 .replaceAll('Recipes:176','Recipes:188').replaceAll('Recipes:116','Recipes:124').replaceAll('Recipes:58','Recipes:62');
fs.writeFileSync('tools/verify-resource12.cjs',s);
fs.copyFileSync('artwork/resource-expansion-v11/journal-contact-sheet.ps1','artwork/resource-expansion-v12/journal-contact-sheet.ps1');
