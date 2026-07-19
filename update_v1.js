const fs = require('fs');
const path = require('path');

const BE_DIR = 'd:/MadhurGram/product-service/src';
const FE_DIR = 'c:/Users/victus/madhurgram-frontend/src';

function replaceInFiles(dir, extFilter) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            replaceInFiles(fullPath, extFilter);
        } else if (extFilter.some(ext => fullPath.endsWith(ext))) {
            let content = fs.readFileSync(fullPath, 'utf8');
            let original = content;
            
            // Replace paths
            content = content.replace(/\/api\/admin/g, '/api/v1/admin');
            content = content.replace(/\/api\/public/g, '/api/v1/public');
            
            if (content !== original) {
                fs.writeFileSync(fullPath, content, 'utf8');
                console.log('Updated:', fullPath);
            }
        }
    }
}

console.log('Updating Backend...');
replaceInFiles(BE_DIR, ['.java']);

console.log('Updating Frontend...');
replaceInFiles(FE_DIR, ['.ts', '.tsx']);

console.log('Done!');
