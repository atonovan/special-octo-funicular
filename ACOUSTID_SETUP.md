# AcoustID API Key Setup

This app uses AcoustID for audio recognition. You need a free API key to enable this feature.

## Getting Your API Key

1. Go to https://acoustid.org/new-application
2. Enter an application name (e.g., "VinylCast Fork")
3. Enter your email address
4. Click "Register"
5. You'll receive an API key immediately

## Adding the API Key

1. Open (or create) the file `local.properties` in the project root
2. Add this line:
   ```
   ACOUSTID_API_KEY=your_actual_api_key_here
   ```
3. Replace `your_actual_api_key_here` with the key you got from AcoustID
4. Save the file
5. Rebuild the app

The `local.properties` file is in `.gitignore` so your API key won't be committed to git.

## Testing

After adding your API key and rebuilding:
- Start streaming audio
- After about 15-30 seconds, you should see track information and album artwork appear
- Check logcat for "Generated fingerprint, querying AcoustID..." to see recognition attempts

## Services Used

- **AcoustID**: Audio fingerprinting and track identification
- **MusicBrainz**: Track metadata (via AcoustID)
- **Cover Art Archive**: Album artwork

All services are free for non-commercial use.
