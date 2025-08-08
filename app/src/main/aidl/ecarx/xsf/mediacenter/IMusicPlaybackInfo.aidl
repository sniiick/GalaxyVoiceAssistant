package ecarx.xsf.mediacenter;

interface IMusicPlaybackInfo {
    String getTitle();
    String getArtist();
    String getAlbum();
    Uri getArtwork();
    Uri getLyric();
    String getLyricContent();
    String getCurrentLyricSentence();
    long getDuration();
    int getPlaybackStatus();
    int getSourceType();
    String getPackageName();
    String getAppName();
    String getUuid();
    PendingIntent getLaunchIntent();
    PendingIntent getPlayerIntent();
    String getIconUri();
}