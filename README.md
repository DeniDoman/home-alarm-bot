# home-alarm-bot

This simple Telegram bot does two things:

- sends commands from the user to the MQTT server
- in case of alarm, it takes photos and videos from your camera via RTSP stream (optional)

This Telegram bot is a part of my home security alarm system, so it might be pretty specific. For example, the list of
commands is hardcoded at this moment, same as MQTT topics names. But it is still usable and may be refactored in the
future.

# Usage

### Requirements and limitations

- Currently, only the x86-64 architecture is supported for photo and video recording. As a result, running it on
  Raspberry Pi and other ARM devices is not possible. It's not a fundamental limitation; it's an optimization choice to
  avoid bundling all ffmpeg
  binaries
  into the docker container. Feel free to raise an issue or PR if you need support of other platforms

### Running the image

```
docker run \
    -d \
    -e TELEGRAM_TOKEN="your_telegram_bot_token" \
    -e CHATS_LIST="12345,67890,-1234567890" \
    -e MQTT_ADDRESS="tcp://192.168.1.1:1883" \
    -e MQTT_USER="mqtt_username" \
    -e MQTT_PASSWORD="mqtt_password" \
    -e MQTT_CLIENT_ID="HomeAlarmBot" \
    -e ALERT_INTERVAL="5" \
    -e RTSP_URL="rtsp://192.168.1.10:554/onvif_camera/profile.0" \
    -e RTSP_USER="rtsp_user" \
    -e RTSP_PASSWORD="rtsp_password" \
    -e RTSP_CLIP_LENGTH="15" \
    -e RTSP_IMAGE_INTERVAL="5" \
    ghcr.io/denidoman/home-alarm-bot:main
```

### Supported commands

HomeAlarmBot supports a total of 7 commands. This implies that only these commands can be sent to the MQTT server; any
other text input from users will be rejected. So, in your automation service, you can handle and interpret these
commands as you want.

The same 7 commands can be sent to the bot from MQTT server, any other text will be shown in your chats, but not
processed. The only processing
that exists in the bot is a starting or stopping sending text alerts, photos and videos to chats.

Below is a list of commands with a corresponding "Start or Stop" column. This column indicates the action (start or
stop) that a specific command triggers when received from the MQTT — mainly whether to start or stop sending alerts,
photos, or videos to chats.

| Command         | Start or Stop |
|-----------------|---------------|
| ENABLED_AUTO    | stop          |
| ENABLED_MANUAL  | stop          |
| DISABLED_AUTO   | stop          |
| DISABLED_MANUAL | stop          |
| ALARM_AUTO      | start         |
| ALARM_MANUAL    | start         |
| GET_STATE       | no action     |

### Keyboard layout

The bot has a keyboard containing only two buttons:

- `✅ ENABLE` - sends `ENABLED_AUTO` command to MQTT
- `❌ DISABLE` - sends `DISABLED_AUTO` command to MQTT

You can send other supported commands manually. Feel free to raise an issue or PR if you want to make the keyboard
configurable.

### MQTT Topics

HomeAlarmBot is interacting with MQTT server using the following topics:

- `securityAlarm/event` for incoming messages, which means the bot listens to this channel
- `homeAlarmBot/message` for outgoing messages, so all your Telegram messages will be sent to this topic

### Environment parameters

Configuration of the bot is done using environment parameters. See the list below:

| Name                | Default      | Description                                                                                                                                                                                                                                                                                                         |
|---------------------|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| TELEGRAM_TOKEN      | \<required\> | Your Telegram bot token                                                                                                                                                                                                                                                                                             |
| CHATS_LIST          | \<required\> | Chat IDs or user IDs to listen for commands and notify about alerts. You can use `@RawDataBot` to get chat ID or your user ID, and avoid using your `@Username` here. ❗Pay attention that in case of chat ID - all participants of the group chat will be able to send commands and view your alerts/photos/videos! |
| MQTT_ADDRESS        | \<required\> | Your MQTT server address should start from `tcp://` for HTTP connections and from `ssl://` for HTTPS. Ensure you include the port in the address. Example: `tcp://192.168.1.1:1883`                                                                                                                                 |
| MQTT_USER           | \<required\> | MQTT user                                                                                                                                                                                                                                                                                                           |
| MQTT_PASSWORD       | \<required\> | MQTT user's password                                                                                                                                                                                                                                                                                                |
| MQTT_CLIENT_ID      | \<required\> | MQTT Client ID (must be unique for the server)                                                                                                                                                                                                                                                                      |
| ALERT_INTERVAL      | 5            | (Optional) How often to send alert messages when the alarm is activated, **in seconds**. Alert messages will be skipped if the parameter is 0 or empty                                                                                                                                                              |
| RTSP_URL            | \<empty\>    | (Optional) Full URL of your RTSP stream (e.g. `rtsp://192.168.1.10:554/onvif_camera/profile.0`). Taking photos and videos will be skipped if the parameter is 0 or empty                                                                                                                                            |
| RTSP_USER           | \<empty\>    | (Optional) RTSP username in case your stream is secured by password                                                                                                                                                                                                                                                 |
| RTSP_PASSWORD       | \<empty\>    | (Optional) RTSP password in case your stream is secured by password                                                                                                                                                                                                                                                 |
| RTSP_CLIP_LENGTH    | 15           | (Optional) Length of recorded video clip, **in seconds**. Clips are recorded in a loop while alarm is active. If the alarm is stopped during the clip recording - it will be sent to chats after end of recording                                                                                                   |
| RTSP_IMAGE_INTERVAL | 5            | (Optional) How often to take and send photos from RTSP stream, **in seconds**. Taking photos will be skipped if the parameter is 0 or empty                                                                                                                                                                         |

### License MIT