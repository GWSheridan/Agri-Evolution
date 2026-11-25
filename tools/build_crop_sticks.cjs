const fs = require('node:fs');
const models = 'common/src/main/resources/assets/agri_evolution/models';
const variants = { wood: 'crop_support', iron: 'iron_crop_support', obsidian: 'obsidian_crop_support' };

function stake(x, z, height) {
    // The vertical grain spans the full height of each face. Alternate swatch halves around the shaft.
    return {
        from: [x, 0, z],
        to: [x + 1.125, height, z + 1.125],
        faces: {
            north: { texture: '#shaft', uv: [0, 0, 8, 16] },
            east: { texture: '#shaft', uv: [8, 0, 16, 16] },
            south: { texture: '#shaft', uv: [0, 0, 8, 16] },
            west: { texture: '#shaft', uv: [8, 0, 16, 16] },
            up: { texture: '#shaft', uv: [4, 0, 8, 4] },
            down: { texture: '#shaft', uv: [8, 12, 12, 16] },
        },
    };
}

for (const [material, id] of Object.entries(variants)) {
    const shaft = `agri_evolution:block/${material}_crop_stick_shaft`;
    const block = {
        parent: 'minecraft:block/block',
        render_type: 'minecraft:cutout',
        textures: { shaft, particle: shaft },
        elements: [stake(3, 3, 12), stake(12, 3, 13), stake(3, 12, 13), stake(12, 12, 12)],
    };
    const item = {
        parent: 'minecraft:item/generated',
        textures: { layer0: `agri_evolution:item/${id}` },
    };
    fs.writeFileSync(`${models}/block/${id}_overlay.json`, `${JSON.stringify(block, null, 2)}\n`);
    fs.writeFileSync(`${models}/item/${id}.json`, `${JSON.stringify(item, null, 2)}\n`);
}
