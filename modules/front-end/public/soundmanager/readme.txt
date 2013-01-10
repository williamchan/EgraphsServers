SoundManager 2 is used to provide the audio player on the Egraph page. In particular, the "360-player" demo project
is used with few modifications. Those modifications are all in 360player.js and they are:

1) playRingColor is changed from "#000" to "#fff" so that the played part of the audio is displayed as white instead of black.
2) useAmplifier is changed from true to false so that the player does not bounce like a low-class boombox.
3) diameter is scaled down by 2 (ie, divided by 2) so that the player is not too large when playing audio.

Full Api documentation is available at:
http://www.schillmania.com/projects/soundmanager2/doc/generated/demo/360-player/script/360player.html
