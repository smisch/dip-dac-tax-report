import fs from 'fs';
import * as uuid from 'uuid';
import jwt from 'jsonwebtoken';
import axios from 'axios';
import qs from 'qs';

// test system
const YOUR_CLIENT_ID = 'TODO';
const privateKey = fs.readFileSync('../private_key.pem', 'utf8');
const SIGNED_XML_FILE_UPLOAD = '../dip-signed.xml';
const BZST_AUDIENCE = 'https://mds-ktst.bzst.bund.de/auth/realms/mds';
const BZST_BASE_URL = 'https://mds-ktst.bzst.bund.de';

// prod system
// const YOUR_CLIENT_ID = 'TODO';
// const privateKey = fs.readFileSync('../private_key.pem', 'utf8');
// const SIGNED_XML_FILE_UPLOAD = '../dip-signed.xml';
// const BZST_AUDIENCE = 'https://mds.bzst.bund.de/auth/realms/mds';
// const BZST_BASE_URL = 'https://mds.bzst.bund.de';

// set this to your data transfer number after a successful 
// upload to check it's protocol status. 
// if empty, will upload `SIGNED_XML_FILE_UPLOAD`.
const EXISTING_DATA_TRANSFER_NUMBER = ""

const payload = {
    iat: Math.floor(Date.now() / 1000),
    jti: uuid.v4(),
    nbf: Math.floor(Date.now() / 1000) - 30,
}
const signOptions = {
    issuer: YOUR_CLIENT_ID,
    subject: YOUR_CLIENT_ID,
    audience: BZST_AUDIENCE,
    expiresIn: "1h",
    algorithm: "RS256",
}
const token = jwt.sign(payload, privateKey, signOptions);

try {
    const authClient = axios.create({
        baseURL: BZST_BASE_URL,
        headers: {
            'content-type': 'application/x-www-form-urlencoded',
            'accept': 'application/json',
        },
    });

    const queryString = qs.stringify({
        grant_type: 'client_credentials',
        scope: 'openid',
        client_assertion_type: 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
        client_assertion: token,
    });
    const authResponse = await authClient.post('/auth/realms/mds/protocol/openid-connect/token', queryString);
    const accessToken = authResponse.data.access_token
    console.log('authResponse', authResponse.status, authResponse.statusText, authResponse.data);

    const dipClient = axios.create({
        baseURL: BZST_BASE_URL,
        headers: {
            'Authorization': `Bearer ${accessToken}`
        },
    });

    let dataTransferNumber;
    if (EXISTING_DATA_TRANSFER_NUMBER) {
        dataTransferNumber = EXISTING_DATA_TRANSFER_NUMBER;
    } else {
        // if we don't have a data transfer number yet, meaning we didn't upload anything yet, 
        // take the signed dip file and upload it now
        const startResponse = await dipClient.post('/dip/start/DAC7');
        dataTransferNumber = startResponse.data; // text/plain
        console.log('startResponse', startResponse.status, startResponse.statusText, startResponse.data)

        const uploadResponse = await dipClient.put(
            `/dip/md/${dataTransferNumber}/xml`, 
            fs.createReadStream(SIGNED_XML_FILE_UPLOAD), {
                headers: {
                    'Content-Type': 'application/octet-stream',
                },
        });
        console.log('uploadResponse', uploadResponse.status, uploadResponse.statusText, uploadResponse.data);

        const finishResponse = await dipClient.patch(`/dip/md/${dataTransferNumber}/finish`);
        console.log('finishResponse', finishResponse.status, finishResponse.statusText, finishResponse.data);
        
        const protocolNumbersResponse = await dipClient.get(`/dip/md/protocolnumbers`);
        console.log('protocolNumbersResponse', protocolNumbersResponse.status, protocolNumbersResponse.statusText, protocolNumbersResponse.data);
    }
    
    try {
        const protocolResponse = await dipClient.get(`/dip/md/${dataTransferNumber}/protocol`);
        console.log('protocolResponse', protocolResponse.status, protocolResponse.statusText, protocolResponse.data);
    } catch (e) {
        console.log('protocolResponse error', e.response.status, e.response.statusText, e.response.data);
    }

    // optionally output all existing data transfer numbers
    const protocolListResponse = await dipClient.get(`/dip/md/protocolnumbers`);
    console.log('protocolListResponse', protocolListResponse.status, protocolListResponse.statusText, protocolListResponse.data);

} catch (error) {
    const { response } = error
    console.log("request failed\n", response?.status, response?.statusText, "\n", response?.data, `${error}`)
}
