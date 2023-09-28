const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const port = process.env.PORT || 3000;
const staticDirectory = path.join(__dirname, 'payload');

if(!fs.existsSync(staticDirectory)) {
	fs.mkdirSync(staticDirectory);
}

app.use((req, res, next) => {
	console.log(`[${new Date().toLocaleString()}] ${req.method} ${req.url}`);
	next();
});

app.use(express.static(staticDirectory));

app.listen(port, () => {
	console.log(`Server is running on port ${port}`);
});