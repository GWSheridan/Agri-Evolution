// Original geometry with dedicated copper trim and existing greenhouse and crop art.
const fs = require('node:fs');
const assets = 'src/main/resources/assets/agri_evolution';
const directions = ['north', 'south', 'east', 'west', 'up', 'down'];
const textures = {
    particle: 'agri_evolution:block/botanical_greenhouse_side',
    casing: 'agri_evolution:block/botanical_greenhouse_side',
    console: 'agri_evolution:block/botanical_greenhouse_front_smart_on',
    crown: 'agri_evolution:block/seed_breeding_station_front',
    copper_rail: 'agri_evolution:block/vertical_greenhouse_copper_rail',
    copper_plate: 'agri_evolution:block/vertical_greenhouse_copper_plate',
    roof: 'agri_evolution:block/botanical_greenhouse_top',
    port: 'agri_evolution:block/irrigation_nozzle',
    soil: 'agri_evolution:block/cultivated_farmland_wet',
    glass: 'minecraft:block/glass',
    tomato: 'agri_evolution:block/tomato_crop_stage7',
    flax: 'agri_evolution:block/azure_flax_crop_stage7',
};

function box(from, to, material, overrides = {}) {
    const faces = Object.fromEntries(directions.map(dir => [dir, { texture: `#${overrides[dir] ?? material}` }]));
    return { from, to, faces };
}

function copperTrim(from, to, overrides = {}, upright = false) {
    const part = box(from, to, 'copper_rail', overrides);
    for (const [side, face] of Object.entries(part.faces)) {
        if (face.texture !== '#copper_rail') continue;
        if (side === 'up' || side === 'down') {
            face.texture = '#copper_plate';
            face.uv = [0, 0, 16, 16];
        } else {
            face.uv = [0, 6, 16, 10];
            if (upright) face.rotation = 90;
        }
    }
    return part;
}

function chamber(crop, upper) {
    const top = upper ? 11 : 15;
    const elements = [copperTrim([0, 0, 0], [16, 2, 16], { up: 'soil' })];
    for (const x of [0, 14]) for (const z of [0, 14]) {
        elements.push(copperTrim([x, 2, z], [x + 2, top, z + 2], {}, true));
    }
    const glass = (from, to, outward) => ({ from, to, faces: { [outward]: { texture: '#glass', uv: [0, 0, 16, 16] } } });
    elements.push(glass([2, 2, 0.6], [14, top, 0.6], 'north'), glass([2, 2, 15.4], [14, top, 15.4], 'south'),
        glass([0.6, 2, 2], [0.6, top, 14], 'west'), glass([15.4, 2, 2], [15.4, top, 14], 'east'));
    for (const angle of [-45, 45]) {
        const face = { texture: `#${crop}`, uv: [0, 0, 16, 16] };
        elements.push({ from: [1.5, 2, 8], to: [14.5, top, 8], rotation: { origin: [8, 8, 8], axis: 'y', angle },
            shade: false, faces: { north: face, south: face } });
    }
    elements.push(upper ? box([0, top, 0], [16, 16, 16], 'casing', { north: 'crown', up: 'roof' })
        : copperTrim([0, top, 0], [16, 16, 16]));
    return elements;
}

const sections = [
    [copperTrim([0, 0, 0], [16, 2, 16]), box([0.5, 2, 0.5], [15.5, 14, 15.5], 'casing', { north: 'console' }),
        copperTrim([0, 14, 0], [16, 16, 16], { up: 'roof' }), box([5, 5, 15.5], [11, 11, 16], 'port')],
    chamber('tomato', false),
    chamber('flax', true),
];
const suffixes = ['base', 'chamber', 'crown'];
const write = (path, data) => fs.writeFileSync(`${assets}/${path}`, `${JSON.stringify(data, null, 2)}\n`);
const variants = {};
sections.forEach((elements, section) => {
    const name = `vertical_greenhouse_${suffixes[section]}`;
    write(`models/block/${name}.json`, { parent: 'minecraft:block/block', render_type: 'minecraft:cutout', textures, elements });
    ['north', 'east', 'south', 'west'].forEach((facing, rotation) => {
        variants[`facing=${facing},section=${section}`] = { model: `agri_evolution:block/${name}`, y: rotation * 90 };
    });
});
write('blockstates/vertical_greenhouse.json', { variants });

// The item is a miniature of the assembled column, using its own inventory scale.
const miniature = sections.flatMap((elements, section) => elements.map(element => {
    const scale = ([x, y, z]) => [8 + (x - 8) * 0.38, (y + section * 16) / 3, 8 + (z - 8) * 0.38];
    const faces = Object.fromEntries(Object.entries(element.faces).map(([side, face]) => [side, { uv: [0, 0, 16, 16], ...face }]));
    const copy = { ...element, from: scale(element.from), to: scale(element.to), faces };
    if (copy.rotation) copy.rotation = { ...copy.rotation, origin: scale(copy.rotation.origin) };
    return copy;
}));
write('models/item/vertical_greenhouse.json', { parent: 'minecraft:block/block', render_type: 'minecraft:cutout', textures,
    elements: miniature, display: { gui: { rotation: [12, 225, 0], scale: [1.05, 1.05, 1.05] } } });
