"""Build disposable Android probe using only the isolated official toolchain."""
from pathlib import Path
import subprocess, zipfile, json, hashlib, os
root=Path(__file__).resolve().parent
tc=Path(os.environ.get('ANDROID_BRIDGE_TOOLCHAIN',str(root/'toolchain'))).resolve()
javac=next((tc/'jdk').rglob('javac.exe')); java=javac.with_name('java.exe');keytool=javac.with_name('keytool.exe')
android=next((tc/'platform').rglob('android.jar')); aapt=next((tc/'build-tools').rglob('aapt2.exe')); bt=aapt.parent
out=root/'build';out.mkdir(exist_ok=True);classes=out/'classes-v10';classes.mkdir(exist_ok=True);dex=out/'dex-v10';dex.mkdir(exist_ok=True)
def run(args):
    # Google's Windows native tools do not consistently accept Unicode absolute paths.
    # Keep the executable absolute, but pass workspace inputs/outputs as ASCII relative paths.
    command=[str(args[0])]
    for arg in args[1:]:
        if isinstance(arg,Path):
            try:arg=arg.relative_to(root)
            except ValueError:
                try:arg=Path(os.path.relpath(arg,root))
                except ValueError:pass
        command.append(str(arg))
    subprocess.run(command,cwd=root,check=True)
sources=list((root/'src').rglob('*.java'))
tests=out/'policy-tests-v10';tests.mkdir(exist_ok=True)
run([javac,'-encoding','UTF-8','-d',tests,root/'src/local/codex/xiaominfcprobe/BridgeGate.java',root/'tests/BridgeGateTest.java'])
run([java,'-cp',tests,'local.codex.xiaominfcprobe.BridgeGateTest'])
run([javac,'-encoding','UTF-8','-d',tests,root/'src/local/codex/xiaominfcprobe/NativeTarget.java',root/'tests/NativeTargetTest.java'])
run([java,'-cp',tests,'local.codex.xiaominfcprobe.NativeTargetTest'])
run([javac,'-encoding','UTF-8','-d',tests,root/'src/local/codex/xiaominfcprobe/GallerySelection.java',root/'tests/GallerySelectionTest.java'])
run([java,'-cp',tests,'local.codex.xiaominfcprobe.GallerySelectionTest'])
run([javac,'-encoding','UTF-8','-d',tests,root/'src/local/codex/xiaominfcprobe/CompletionWatch.java',root/'tests/CompletionWatchTest.java'])
run([java,'-cp',tests,'local.codex.xiaominfcprobe.CompletionWatchTest'])
run([javac,'-encoding','UTF-8','-d',tests,root/'src/local/codex/xiaominfcprobe/PollDeadline.java',root/'tests/PollDeadlineTest.java'])
run([java,'-cp',tests,'local.codex.xiaominfcprobe.PollDeadlineTest'])
run([javac,'-encoding','UTF-8','-source','8','-target','8','-classpath',android,'-d',classes,*sources])
run([java,'-cp',bt/'lib'/'d8.jar','com.android.tools.r8.D8','--lib',android,'--min-api','26','--output',dex,*classes.rglob('*.class')])
unsigned=out/'probe-unsigned.apk'
compiled=out/'resources-v10.zip'
run([aapt,'compile','--dir',root/'res','-o',compiled])
run([aapt,'link','-I',android,'--manifest',root/'AndroidManifest.xml','--min-sdk-version','26','--target-sdk-version','35','-o',unsigned,compiled])
with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as z:z.write(dex/'classes.dex','classes.dex')
aligned=out/'probe-aligned.apk';run([bt/'zipalign.exe','-f','4',unsigned,aligned])
key=out/'disposable-debug.keystore'
if not key.exists():run([keytool,'-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','probe','-keyalg','RSA','-keysize','2048','-validity','365','-dname','CN=Disposable Xiaomi Bridge Probe'])
apk=out/'XiaomiNfcBridge-probe-v10.apk'
run([java,'-jar',bt/'lib'/'apksigner.jar','sign','--ks',key,'--ks-pass','pass:android','--out',apk,aligned])
run([java,'-jar',bt/'lib'/'apksigner.jar','verify','--verbose',apk])
print(json.dumps({'apk':str(apk),'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'bytes':apk.stat().st_size}))
