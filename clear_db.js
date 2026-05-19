const { MongoClient } = require('mongodb');
const uri = 'YOUR_MONGODB_URI';
const client = new MongoClient(uri);

async function run() {
  try {
    await client.connect();
    const db = client.db('bookstore');
    const res = await db.collection('books').deleteMany({});
    console.log('Successfully deleted ' + res.deletedCount + ' old books from the database!');
  } finally {
    await client.close();
  }
}
run().catch(console.dir);
