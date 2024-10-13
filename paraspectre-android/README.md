```bash
git submodule update --init --recursive
./build.sh
adb install app/build/outputs/apk/app-debug.apk
# *enable in Xposed Installer*
adb reboot
adb install -r app/build/outputs/apk/app-debug.apk # enables INTERNET granting
```

```bash
./scripts/prepnet.sh
./scripts/logscan.sh 'NCC|foobar'
```

Start the ParaSpectre app and enable both the server and proxy.
Copy the API key from the logs:
```
08-17 15:57:06.794  8170  8508 I PS/WebApp: API key: ZZZZZZZZZZZZZZZZZZZZZZ==
```

Visit <https://127.0.0.1:8088> and enter the API key.

Add hooks and run the REPL listener:
```bash
cd ../pinglistener
./run.sh
```
