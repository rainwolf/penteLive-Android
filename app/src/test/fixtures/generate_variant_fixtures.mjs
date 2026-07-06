// Fixture generator for the VariantReferee regression net (games 11 Poof-Pente,
// 15 Boat-Pente, 25 O-Pente).
//
// It drives the AUTHORITY referee, react_mmai/src/Classes/GameClass.js, over a
// fixed-seed set of pseudo-random *legal* games and records, per game, the move
// list plus the final board / captures / winner / gameOver. The companion JUnit
// test (VariantRefereeFixtureTest) replays each recorded move list through the
// extracted Java VariantReferee and asserts identical board / captures / winner,
// pinning the mobile referee to its web authority.
//
// Run ONCE and commit the emitted JSON (checked in under
// app/src/test/resources/variant_fixtures/). Regenerate only when the referee
// semantics intentionally change:
//     node app/src/test/fixtures/generate_variant_fixtures.mjs
//
// GameClass.js is an ES module (`export class Game`) inside a package with no
// "type":"module", so - exactly like react_mmai/tests/gameclass.test.mjs - we
// copy it to a temp *.mjs and dynamic-import that.

import { readFileSync, writeFileSync, mkdirSync, unlinkSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { pathToFileURL, fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));

// react_mmai is a sibling project of penteLive-Android under pente.org-project.
// Override with GAMECLASS=/path/to/GameClass.js if the layout differs.
const gameClassPath = process.env.GAMECLASS ||
    join(here, '..', '..', '..', '..', '..', 'react_mmai', 'src', 'Classes', 'GameClass.js');

const outDir = join(here, '..', 'resources', 'variant_fixtures');

const SIZE = 19;
const SEED = 20260706;      // fixed seed -> reproducible fixtures
const GAMES_PER_VARIANT = 200;
const MAX_MOVES = 80;       // capped move count per game
const FRONTIER_BIAS = 0.8;  // fraction of moves drawn from the frontier
const VARIANTS = [11, 15, 25];

// Deterministic PRNG (mulberry32).
function mulberry32(a) {
    return function () {
        a |= 0;
        a = (a + 0x6D2B79F5) | 0;
        let t = Math.imul(a ^ (a >>> 15), 1 | a);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

const XY = (x, y) => y * SIZE + x;

async function loadGame() {
    const src = readFileSync(gameClassPath, 'utf8');
    const tmp = join(tmpdir(), `gameclass.gen.${process.pid}.mjs`);
    writeFileSync(tmp, src);
    const mod = await import(pathToFileURL(tmp).href);
    unlinkSync(tmp);
    return mod.Game;
}

// Pick a legal (empty-cell) move. To produce dense, capture/poof-rich and
// occasionally-winning games we draw 80% of moves from the "frontier" (empty
// cells adjacent to an existing stone); the rest, and the opening move, are
// uniform over all empty cells. rated=false means there are never -1 cells.
function pickMove(g, rnd) {
    const B = g.abstractBoard;
    const empties = [];
    const frontier = [];
    for (let x = 0; x < SIZE; x++) {
        for (let y = 0; y < SIZE; y++) {
            if (B[x][y] !== 0) continue;
            empties.push([x, y]);
            let adj = false;
            for (let dx = -1; dx <= 1 && !adj; dx++) {
                for (let dy = -1; dy <= 1; dy++) {
                    if (dx === 0 && dy === 0) continue;
                    const nx = x + dx, ny = y + dy;
                    if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE &&
                        (B[nx][ny] === 1 || B[nx][ny] === 2)) {
                        adj = true;
                        break;
                    }
                }
            }
            if (adj) frontier.push([x, y]);
        }
    }
    if (empties.length === 0) return -1;
    let pool;
    if (g.moves.length === 0 || frontier.length === 0) {
        pool = empties;
    } else {
        pool = (rnd() < FRONTIER_BIAS) ? frontier : empties;
    }
    const [x, y] = pool[Math.floor(rnd() * pool.length)];
    return XY(x, y);
}

function playOneGame(Game, variant, rnd) {
    const g = new Game();
    g.setGame(variant);
    g.reset();
    g.rated = false;
    const moves = [];
    for (let step = 0; step < MAX_MOVES; step++) {
        const move = pickMove(g, rnd);
        if (move < 0) break;
        g.addMove(move);
        moves.push(move);
        // addMove only sets the winner for row wins (11) directly; capture wins
        // (11/15/25) and boat-style row survival (15/25) live in isGameOver, which
        // we must call to finalize and to detect the end of the game.
        if (g.isGameOver()) break;
    }
    const gameOver = g.isGameOver();          // idempotent finalize
    const winner = g.winner ? g.winner : 0;   // undefined -> 0
    const board = new Array(SIZE * SIZE);
    for (let x = 0; x < SIZE; x++) {
        for (let y = 0; y < SIZE; y++) {
            board[x * SIZE + y] = g.abstractBoard[x][y]; // x-major: index = x*19 + y
        }
    }
    return {
        moves,
        white: g.captures[1],   // captures[1] = stones lost by white == whiteCaptures
        black: g.captures[2],   // captures[2] = stones lost by black == blackCaptures
        winner,                 // 0 none / 1 white / 2 black (== VariantReferee.winner)
        gameOver,
        board,
    };
}

async function main() {
    const Game = await loadGame();
    mkdirSync(outDir, { recursive: true });
    for (const variant of VARIANTS) {
        // A distinct seed per variant keeps the three files independent yet fixed.
        const rnd = mulberry32(SEED + variant);
        const games = [];
        let ended = 0, rowWins = 0, captureTotal = 0;
        for (let i = 0; i < GAMES_PER_VARIANT; i++) {
            const rec = playOneGame(Game, variant, rnd);
            games.push(rec);
            if (rec.gameOver) ended++;
            captureTotal += rec.white + rec.black;
        }
        const fixture = {
            variant,
            seed: SEED + variant,
            count: games.length,
            maxMoves: MAX_MOVES,
            note: 'Authority: react_mmai/src/Classes/GameClass.js. board is x-major ' +
                '(index=x*19+y); 0 empty/1 white/2 black. white/black = captures[1]/[2] ' +
                '(stones lost). winner 0/1/2.',
            games,
        };
        const outPath = join(outDir, `game_${variant}.json`);
        writeFileSync(outPath, JSON.stringify(fixture) + '\n');
        const decided = games.filter(g => g.winner !== 0).length;
        console.log(`variant ${variant}: ${games.length} games, ${ended} gameOver, ` +
            `${decided} decided, ${captureTotal} total captures -> ${outPath}`);
    }
}

main().catch(e => { console.error(e); process.exit(1); });
