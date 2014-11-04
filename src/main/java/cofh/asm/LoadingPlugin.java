package cofh.asm;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.minecraft.launchwrapper.LaunchClassLoader;

@IFMLLoadingPlugin.TransformerExclusions({ "cofh.asm." })
public class LoadingPlugin implements IFMLLoadingPlugin {

	public static final String MC_VERSION = "[1.7.10]";
	public static ArrayList<String> transformersList = new ArrayList<String>();
	public static boolean runtimeDeobfEnabled = false;
	public static ASMDataTable ASM_DATA = null;
	public static LaunchClassLoader loader = null;

	// Initialize SubMod transformers
	static {

		versionCheck(MC_VERSION, "CoFHCore");
		attemptClassLoad("cofh.asm.TransformerCore", "Failed to find Main Transformer! Critical Issue!");
		attemptClassLoad("cofh.asm.PCCASMTransformer", "Failed to find Secondary Transformer! Critical Issue!");
	}

	public static void versionCheck(String reqVersion, String mod) {

		String mcVersion = (String) FMLInjectionData.data()[4];
		if (!VersionParser.parseRange(reqVersion).containsVersion(new DefaultArtifactVersion(mcVersion))) {
			String err = "This version of " + mod + " does not support Minecraft version " + mcVersion;
			System.err.println(err);

			JEditorPane ep = new JEditorPane("text/html", "<html>" + err
					+ "<br>Remove it from your coremods folder and check <a href=\"http://teamcofh.com/\">here</a> for updates" + "</html>");

			ep.setEditable(false);
			ep.setOpaque(false);
			ep.addHyperlinkListener(new HyperlinkListener() {

				@Override
				public void hyperlinkUpdate(HyperlinkEvent event) {

					try {
						if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
							Desktop.getDesktop().browse(event.getURL().toURI());
						}
					} catch (Exception e) {
					}
				}
			});
			JOptionPane.showMessageDialog(null, ep, "Fatal error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	public LoadingPlugin() {

		// DepLoader.load();
	}

	public static void attemptClassLoad(String className, String failMessage) {

		try {
			Class.forName(className, false, LoadingPlugin.class.getClassLoader());
			transformersList.add(className);
		} catch (Throwable e) {
			FMLLog.warning(failMessage);
		}
	}

	@Override
	public String getAccessTransformerClass() {

		return "cofh.asm.PCCAccessTransformer";
	}

	@Override
	public String[] getASMTransformerClass() {

		return transformersList.toArray(new String[2]);
	}

	@Override
	public String getModContainerClass() {

		return CoFHDummyContainer.class.getName();
	}

	@Override
	public String getSetupClass() {

		return CoFHDummyContainer.class.getName();
	}

	@Override
	public void injectData(Map<String, Object> data) {

		runtimeDeobfEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
		if (data.containsKey("coremodLocation")) {
			myLocation = (File) data.get("coremodLocation");
		}
	}

	public File myLocation;

	public static class CoFHDummyContainer extends DummyModContainer implements IFMLCallHook {

		public CoFHDummyContainer() {

			super(new ModMetadata());
			ModMetadata md = getMetadata();
			md.autogenerated = true;
			md.authorList = Arrays.asList("skyboy026");
			md.modId = "<CoFH ASM>";
			md.name = md.description = "CoFH ASM Data Initialization";
			md.version = "000";
		}

		@Override
		public boolean registerBus(EventBus bus, LoadController controller) {

			bus.register(this);
			return true;
		}

		@Subscribe
		public void construction(FMLConstructionEvent evt) {

			ASM_DATA = evt.getASMHarvestedData();
			PCCASMTransformer.scrapeData(ASM_DATA);
		}

		@Override
		public Void call() throws Exception {

			return null;
		}

		@Override
		public void injectData(Map<String, Object> data) {

			loader = (LaunchClassLoader) data.get("classLoader");
		}
	}

}
