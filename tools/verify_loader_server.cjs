const assert = require('node:assert/strict');
const net = require('node:net');
const { once } = require('node:events');
const { setTimeout: delay } = require('node:timers/promises');

async function checkServer(port, password, phase) {
    const socket = net.connect(port, '127.0.0.1');
    socket.setTimeout(15000, () => socket.destroy(new Error('Server console timed out')));
    await once(socket, 'connect');
    const chunks = socket[Symbol.asyncIterator]();
    let pending = Buffer.alloc(0);
    let nextId = 0;

    async function consoleCommand(type, command) {
        const payload = Buffer.from(command, 'utf8');
        const packet = Buffer.alloc(payload.length + 14);
        const id = ++nextId;
        packet.writeInt32LE(packet.length - 4);
        packet.writeInt32LE(id, 4);
        packet.writeInt32LE(type, 8);
        payload.copy(packet, 12);
        socket.write(packet);

        while (true) {
            if (pending.length >= 4) {
                const length = pending.readInt32LE() + 4;
                assert.ok(length >= 14 && length <= 65536, 'Invalid console response length');
                if (pending.length >= length) {
                    const responseId = pending.readInt32LE(4);
                    const reply = pending.toString('utf8', 12, length - 2);
                    pending = pending.subarray(length);
                    assert.notEqual(responseId, -1, 'Console authentication failed');
                    if (responseId === id) return reply.trim();
                    continue;
                }
            }
            const { value, done } = await chunks.next();
            assert.ok(!done, 'Server closed its console connection');
            pending = Buffer.concat([pending, value]);
        }
    }

    async function expect(command, pattern) {
        const reply = await consoleCommand(2, command);
        assert.match(reply, pattern, command);
        return reply;
    }

    async function awaitCheck(command, pattern) {
        for (let attempt = 0; attempt < 25; attempt++) {
            const reply = await consoleCommand(2, command);
            if (pattern.test(reply)) return reply;
            await delay(250);
        }
        return expect(command, pattern);
    }

    try {
        await consoleCommand(3, password);
        if (phase === 'seed') {
            await consoleCommand(2, 'forceload add 0 0');
            await awaitCheck('execute if loaded 8 80 8', /Test passed/);
            await expect('gamerule randomTickSpeed 0', /now set to: 0/);
            await expect('setblock 1 78 1 agri_evolution:cultivated_farmland[moisture=7]', /Changed the block/);
            await expect('setblock 1 79 1 agri_evolution:agri_crop[age=7]', /Changed the block/);
            await expect('execute if block 1 79 1 agri_evolution:agri_crop[age=7]', /Test passed/);
            await expect('execute if data block 1 79 1 id', /Test passed/);
            await expect('loot spawn 1 79 1 loot agri_evolution:blocks/agri_crop', /Dropped \d+ item/);
            await expect('setblock 2 80 1 agri_evolution:seed_vault', /Changed the block/);
            await expect('execute if data block 2 80 1 id', /Test passed/);

            // Form a valid shell while ticks are frozen so early pieces cannot dismantle it.
            await consoleCommand(2, 'tick freeze');
            for (let x = 0; x < 3; x++) {
                for (let y = 0; y < 3; y++) {
                    for (let z = 0; z < 3; z++) {
                        await expect(`setblock ${x + 7} ${y + 79} ${z + 7} agri_evolution:water_reservoir[part_x=${x},part_y=${y},part_z=${z}]`, /Changed the block/);
                    }
                }
            }
            await expect('data merge block 8 80 8 {Renewable:1b,Water:30000}', /Modified block data/);
            await consoleCommand(2, 'tick unfreeze');
            await expect('tick sprint 415', /sprint/i);
            await awaitCheck('data get block 8 80 8 Water', /32000$/);
            await expect('execute if block 9 80 8 agri_evolution:water_reservoir[fill=4]', /Test passed/);
        } else {
            await awaitCheck('execute if loaded 8 80 8', /Test passed/);
            await expect('data get block 8 80 8 Water', /32000$/);
            await expect('data get block 8 80 8 Renewable', /1b$/);
            await expect('execute if block 1 79 1 agri_evolution:agri_crop[age=7]', /Test passed/);
            await expect('execute if block 2 80 1 agri_evolution:seed_vault', /Test passed/);
        }
        await expect('save-all flush', /Saved the game/);
        console.log(`Dedicated server ${phase} checks passed`);
    } finally {
        await consoleCommand(2, 'stop').catch(() => {});
        socket.destroy();
    }
}

if (require.main === module) {
    const [port, phase] = process.argv.slice(2);
    if (!/^\d+$/.test(port ?? '') || !['seed', 'reload'].includes(phase) || !process.env.AGRI_TEST_RCON) {
        throw new Error('Supply a localhost port, seed or reload, and the AGRI_TEST_RCON environment variable');
    }
    checkServer(Number(port), process.env.AGRI_TEST_RCON, phase).catch(error => {
        console.error(error);
        process.exitCode = 1;
    });
}

module.exports = checkServer;
