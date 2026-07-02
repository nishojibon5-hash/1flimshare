// ===================================================================
// FLIXBUZZ CREATOR PORTAL NODE.JS EXPRESS BACKEND (server.js)
// ===================================================================

const express = require('express');
const cors = require('cors');
const axios = require('axios');
const crypto = require('crypto');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(express.json());
app.use(cors({
    origin: '*', // Allow all client connections
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization', 'api-key']
}));

// Real Infrastructure Details
const MONGO_APP_ID = process.env.MONGO_APP_ID || "data-flixbuzz-pqrst";
const MONGO_API_KEY = process.env.MONGO_API_KEY || "65f012cf01bcdc6179abcf9e123abcde";
const DATA_SOURCE = "Cluster0";
const DATABASE_NAME = "flixbuzz";
const DOOD_API_KEY = process.env.DOOD_API_KEY || "569149jtwoitkto1zmwf1s";

// Helper function to query MongoDB Atlas Data API
async function callAtlasAPI(action, collection, payload = {}) {
    const url = `https://data.mongodb-api.com/app/${MONGO_APP_ID}/endpoint/data/v1/action/${action}`;
    const data = {
        dataSource: DATA_SOURCE,
        database: DATABASE_NAME,
        collection: collection,
        ...payload
    };

    try {
        const response = await axios.post(url, data, {
            headers: {
                'Content-Type': 'application/json',
                'api-key': MONGO_API_KEY
            }
        });
        return response.data;
    } catch (error) {
        console.error(`Atlas API Error on [${action}] for collection [${collection}]:`, error.message);
        throw error;
    }
}

// 1. Root Handshake check
app.get('/api/status', (req, res) => {
    res.json({
        status: "active",
        service: "Flixbuzz Ingest Backend",
        timestamp: new Date(),
        database: "MongoDB Atlas Connected",
        version: "v1.0"
    });
});

// 2. Register new channel account
app.post('/api/auth/register', async (req, res) => {
    try {
        const { username, email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ error: "Email & Secure Password are required!" });
        }

        // Check if user exists
        const existing = await callAtlasAPI('findOne', 'users', {
            filter: { email: email }
        });

        if (existing && existing.document) {
            return res.status(400).json({ error: "এই জিমেইল আইডি দিয়ে ইতোমধ্যে একাউন্ট খোলা আছে!" });
        }

        const password_hash = crypto.createHash('sha256').update(password).digest('hex');

        const profile = {
            name: username || "Guest Creator",
            username: username || "Guest Creator",
            email: email,
            password_hash: password_hash,
            balance: 250.00,
            storageUsedBytes: 10485760, // 10MB default
            storageTotalBytes: 107374182400, // 100GB
            viewsLifetime: 1000,
            blockedUserIds: "",
            channelName: username || "New Creator Channel",
            channelHandle: "@" + (username || "creator").toLowerCase().replace(/\s+/g, ''),
            channelLogoUrl: "",
            channelBannerUrl: "",
            channelBio: "স্বাগতম ফ্লিক্সবাজে। ভিডিওগুলো দেখতে চ্যানেল সাবস্ক্রাইব করে রাখুন!",
            creatorLevel: 1,
            creatorExperience: 100,
            subscribersCount: 2
        };

        const result = await callAtlasAPI('insertOne', 'users', {
            document: profile
        });

        res.status(201).json({
            success: true,
            message: "চ্যানেল একাউন্ট সফলভাবে নিবন্ধিত হয়েছে!",
            insertedId: result.insertedId,
            profile: profile
        });
    } catch (e) {
        res.status(500).json({ error: "সার্ভারে নিবন্ধন প্রসেস করতে ব্যর্থ হয়েছে।", details: e.message });
    }
});

// 3. User login validation
app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ error: "Email & Password are required" });
        }

        const result = await callAtlasAPI('findOne', 'users', {
            filter: { email: email }
        });

        if (!result || !result.document) {
            return res.status(404).json({ error: "এই ইমেইল দিয়ে কোনো ক্রিয়েটর একাউন্ট খুঁজে পাওয়া যায়নি!" });
        }

        const doc = result.document;
        const computedHash = crypto.createHash('sha256').update(password).digest('hex');

        if (doc.password_hash === computedHash) {
            res.json({
                success: true,
                message: "লগইন সফল হয়েছে!",
                profile: doc
            });
        } else {
            res.status(401).json({ error: "ভুল পাসওয়ার্ড! অনুগ্রহ করে সঠিক পাসওয়ার্ড দিন।" });
        }
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// 4. Fetch video listings
app.get('/api/videos', async (req, res) => {
    try {
        const result = await callAtlasAPI('find', 'videos');
        res.json(result.documents || []);
    } catch (e) {
        res.status(500).json({ error: "ভিডিও তালিকা পেতে সমস্যা হয়েছে।", details: e.message });
    }
});

// 5. Publish video
app.post('/api/videos/add', async (req, res) => {
    try {
        const { filecode, title, category, videoUrl, durationSeconds, creatorEmail, description, thumbnailUrl } = req.body;
        if (!filecode || !title || !videoUrl) {
            return res.status(400).json({ error: "Missing required video properties check!" });
        }

        const document = {
            filecode,
            title,
            category: category || "UGC Video",
            tags: "web, express",
            videoUrl,
            durationSeconds: parseInt(durationSeconds) || 300,
            views: 0,
            likes: 0,
            isPrivate: false,
            creatorEmail: creatorEmail || "salman016500@gmail.com",
            localEarnings: 0.00,
            description: description || "",
            thumbnailUrl: thumbnailUrl || ""
        };

        const result = await callAtlasAPI('insertOne', 'videos', { document });
        res.status(201).json({ success: true, insertedId: result.insertedId, document });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// 6. Fetch profiles
app.get('/api/user/:email', async (req, res) => {
    try {
        const email = req.params.email;
        const result = await callAtlasAPI('findOne', 'users', {
            filter: { email: email }
        });

        if (result && result.document) {
            res.json(result.document);
        } else {
            res.status(404).json({ error: "Profile not found" });
        }
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// 7. Sync user changes
app.post('/api/user/save', async (req, res) => {
    try {
        const profile = req.body;
        const result = await callAtlasAPI('updateOne', 'users', {
            filter: { email: profile.email },
            update: {
                $set: {
                    username: profile.username,
                    channelName: profile.channelName,
                    channelHandle: profile.channelHandle,
                    channelBio: profile.channelBio
                }
            }
        });
        res.json({ success: true, result });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// 8. Proxy DoodStream remote upload to prevent browser CORS
app.post('/api/doodstream/remote-upload', async (req, res) => {
    try {
        const { url, title, key } = req.body;
        const apiKey = key || DOOD_API_KEY;

        const response = await axios.get(`https://doodapi.com/api/upload/url?key=${apiKey}&url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}`);
        res.json(response.data);
    } catch (e) {
        res.status(500).json({ error: "ডুডস্ট্রিম এপিআই কল করার সময় সার্ভার এরর হয়েছে।", details: e.message });
    }
});

// Start Express Listener
app.listen(PORT, () => {
    console.log(`==============================================`);
    console.log(`🚀 Flixbuzz Backend Running on Port: ${PORT}`);
    console.log(`==============================================`);
});
