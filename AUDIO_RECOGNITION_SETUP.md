# Audio Recognition Setup Guide

This fork of Vinyl Cast includes audio recognition powered by AcoustID, MusicBrainz, and Chromaprint to automatically identify songs from your vinyl records and display album artwork.

## Features

- **Automatic Song Recognition**: Identifies tracks playing from your vinyl in real-time
- **Album Artwork Display**: Shows album art in the app, notification, and Chromecast
- **Free & Open Source**: Uses completely free APIs (AcoustID, MusicBrainz, Cover Art Archive)

## Setup Instructions

### 1. Get Your AcoustID API Key

1. Visit [https://acoustid.org/new-application](https://acoustid.org/new-application)
2. Sign in or create a free account
3. Fill out the application form:
   - **Name**: `Vinyl Cast` (or your custom app name)
   - **Version**: Current version from the app
   - **Website**: Optional (can be your GitHub fork URL)
4. Click "Create Application"
5. Copy your API key

### 2. Configure the API Key

1. Copy the example configuration file:
   ```bash
   cp local.properties.example local.properties
   ```

2. Open `local.properties` in the project root directory

3. Replace `YOUR_API_KEY_HERE` with your actual AcoustID API key:
   ```properties
   ACOUSTID_API_KEY=your-actual-api-key-here
   ```

**Note:** The `local.properties` file is git-ignored for security and will never be committed to version control.

### 3. Build and Run

That's it! Build and run the app. Audio recognition will start automatically when you begin streaming vinyl.

## How It Works

1. **Audio Capture**: The app captures 15 seconds of audio from your vinyl every 30 seconds
2. **Fingerprinting**: Chromaprint generates an acoustic fingerprint of the audio
3. **Recognition**: The fingerprint is sent to AcoustID API to identify the song
4. **Metadata Retrieval**: Song info is fetched from MusicBrainz
5. **Artwork Display**: Album art is downloaded from Cover Art Archive and displayed in:
   - Main app UI
   - Notification
   - Chromecast receiver

## Usage Limits

- **AcoustID Free Tier**:
  - Non-commercial use: Unlimited and free
  - Commercial use: First 10,000 searches free per month
  - No credit card required

## Privacy

All audio recognition is done via secure HTTPS requests to:
- `api.acoustid.org` - Audio fingerprint matching
- `musicbrainz.org` - Song metadata
- `coverartarchive.org` - Album artwork

No audio files are uploaded - only acoustic fingerprints (small compressed representations).

## Troubleshooting

### Recognition Not Working
- Ensure you have internet connectivity
- Check that your API key is correctly configured
- Make sure the audio is clear and not too quiet

### No Album Artwork
- Some releases may not have artwork in Cover Art Archive
- Artwork requires a valid MusicBrainz release ID from recognition

### Rate Limiting
- If you exceed the free tier limits, consider subscribing to AcoustID paid plans
- Default recognition interval is 30 seconds to minimize API usage

## Attribution

This audio recognition feature uses:
- **AcoustID** - https://acoustid.org/ - Audio fingerprinting service
- **Chromaprint** - https://github.com/acoustid/chromaprint - Acoustic fingerprinting library
- **MusicBrainz** - https://musicbrainz.org/ - Open music database
- **Cover Art Archive** - https://coverartarchive.org/ - Free album artwork

Based on the original **Vinyl Cast** by Allen Schober - https://github.com/aschober/vinyl-cast

## License

This fork maintains the original MIT License from Vinyl Cast. See LICENSE file for details.
