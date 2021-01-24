package virtualBox.AwesomeMachine;

import java.util.ArrayList;
import java.util.List;

import org.virtualbox_6_1.AccessMode;
import org.virtualbox_6_1.CleanupMode;
import org.virtualbox_6_1.DeviceType;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.IMedium;
import org.virtualbox_6_1.IProgress;
import org.virtualbox_6_1.ISession;
import org.virtualbox_6_1.IVirtualBox;
import org.virtualbox_6_1.IVirtualBoxErrorInfo;
import org.virtualbox_6_1.LockType;
import org.virtualbox_6_1.MediumVariant;
import org.virtualbox_6_1.StorageBus;
import org.virtualbox_6_1.VBoxException;
import org.virtualbox_6_1.VirtualBoxManager;

public class AwesomeMachine {
	private String nameMachine = "betaMachine";
	private String nameHDController = "Hard Disk Controller";
	private String nameDVDController = "DVD Controller";

	public void createVMbeta(VirtualBoxManager virtualBoxManager, IVirtualBox iVirtualBox) {

		ISession session = virtualBoxManager.getSessionObject();
		// Before creating, first delete if exist another machine already created
		deleteMachine(virtualBoxManager, iVirtualBox, session);

		IMachine betaMachine = iVirtualBox.createMachine(null, nameMachine, null, null, null);

		betaMachine.setMemorySize(1024L);
		betaMachine.setOSTypeId("Fedora_64");

		iVirtualBox.registerMachine(betaMachine);

		betaMachine.lockMachine(session, LockType.Write);

		IMachine sessionMachine = session.getMachine();
		// IF SOMETHING GOES WRONG, THE MACHINE HAS TO BE UNLOCK!
		try {
			IMedium hardDisk = iVirtualBox.createMedium("vdi",
					"C:\\Users\\Desktop\\VirtualBox VMs\\TestMachine\\HardDiskBetaMachine.vdi", AccessMode.ReadWrite,
					DeviceType.HardDisk);
//            IMedium hd = iVirtualBox.openMedium("C:\\Users\\Desktop\\VirtualBox VMs\\TestMachine\\TestMachineBeta.vdi", DeviceType.HardDisk, AccessMode.ReadWrite, Boolean.FALSE);
			List<MediumVariant> mediumVariants = new ArrayList<MediumVariant>();

			mediumVariants.add(MediumVariant.Standard);
			// 15Gb

			IProgress iProgress = hardDisk.createBaseStorage(15L * 1024L * 1024L * 1024L, mediumVariants);
			iProgress.waitForCompletion(-1);
			Integer resultCode = iProgress.getResultCode();
			System.out.println("COBE MESSAGE: ResultCode: " + resultCode);
			sessionMachine.addStorageController(nameHDController, StorageBus.SATA);

			sessionMachine.attachDevice(nameHDController, 0, 0, DeviceType.HardDisk, hardDisk);

			// For dvd Image
			sessionMachine.addStorageController(nameDVDController, StorageBus.IDE);

			IMedium dvdImage = iVirtualBox.openMedium(
					"C:\\Users\\Desktop\\Downloads\\Fedora-Server-dvd-x86_64-33-1.2.iso", DeviceType.DVD,
					AccessMode.ReadOnly, Boolean.FALSE);

			sessionMachine.attachDevice(nameDVDController, 1, 0, DeviceType.DVD, dvdImage);
//            sessionMachine.mountMedium("COBE DVD Controller", 1, 0, dvdImage, Boolean.FALSE);
			sessionMachine.setBootOrder(1L, DeviceType.DVD);
			sessionMachine.saveSettings();

//            deleteMachine(virtualBoxManager, iVirtualBox, session);

		} catch (Exception e) {
			e.printStackTrace();
		}
		session.unlockMachine();

	}

	public void deleteMachine(VirtualBoxManager virtualBoxManager, IVirtualBox iVirtualBox, ISession iSession) {
		try {
			IMachine machineToDelete = iVirtualBox.findMachine(nameMachine);
			machineToDelete.lockMachine(iSession, LockType.Write);
			iSession.unlockMachine();
			List<IMedium> media = machineToDelete.unregister(CleanupMode.DetachAllReturnHardDisksOnly);
			for (IMedium iMedium : media) {
				System.out.println(iMedium.getName() + " - " + iMedium.getDescription() + " - " + iMedium.getFormat());
			}
			machineToDelete.deleteConfig(media);
			System.out
					.println("COBE MESSAGE: The previous machine '" + nameMachine + "' has been deleted successfully ");
		} catch (Exception e) {
			System.out.println("COBE MESSAGE: Couldn't find machine " + e.getMessage());
		}
	}

	public void printErrorInfo(VBoxException e) {
		System.out.println("VBox error: " + e.getMessage());
		System.out.println("Error cause message: " + e.getCause());
		System.out.println("Overall result code: " + Integer.toHexString(e.getResultCode()));
		int i = 1;
		for (IVirtualBoxErrorInfo ei = e.getVirtualBoxErrorInfo(); ei != null; ei = ei.getNext(), i++) {
			System.out.println("Detail information #" + i);
			System.out.println("Error mesage: " + ei.getText());
			System.out.println("Result code:  " + Integer.toHexString(ei.getResultCode()));
			// optional, usually provides little additional information:
			System.out.println("Component:    " + ei.getComponent());
			System.out.println("Interface ID: " + ei.getInterfaceID());
		}
	}

	public void turnOn_VM(VirtualBoxManager virtualBoxManager, IVirtualBox vbox) {
		// from the list returned by getMachines we get the third one	
		IMachine cobe = vbox.findMachine("betaMachine");		
		System.out.println("\nAttempting to start VM '" + cobe.getName() + "'");

		/*
		 * doc:The ISession interface represents a client process and allows for locking
		 * virtual machines (represented by IMachine objects) to prevent conflicting
		 * changes to the machine.
		 * 
		 * Any caller wishing to manipulate a virtual machine needs to create a session
		 * object first, which lives in its own process space. Such session objects are
		 * then associated with IMachine objects living in the VirtualBox server process
		 * to coordinate such changes
		 */
		ISession session = virtualBoxManager.getSessionObject();
		ArrayList<String> env = new ArrayList<String>();
		/*
		 * To launch a virtual machine, you call IMachine::launchVMProcess(). In doing
		 * so, the caller instructs the VirtualBox engine to start a new process with
		 * the virtual machine in it, since to the host, each virtual machine looks like
		 * single process, even if it has hundreds of its own processes inside. (This
		 * new VM process in turn obtains a write lock on the machine, as described
		 * above, to prevent conflicting changes from other processes; this is why
		 * opening another session will fail while the VM is running.)
		 */
		// Param 1 session object
		// param 2 session type
		// Param 3 possibly environment setting
		IProgress launchVMProcess = cobe.launchVMProcess(session, "gui", env);
//	        IProgress p = iMachine.launchVMProcess(session, "gui", env);
		// give the proces 10 secs
		System.out.println("launching Vm!");
		launchVMProcess.waitForCompletion(-1);
//	        progressStatus(virtualBoxManager, p, 10000);

//	        session.unlockMachine();
		// process system event queue
//	        virtualBoxManager.waitForEvents(0);
	}

	public void turnOff_VM(VirtualBoxManager virtualBoxManager, IVirtualBox vbox) {
		/*
		 * doc:Attempts to find a virtual machine given its name or UUID.
		 */
		IMachine machine = vbox.findMachine(nameMachine);

		ISession session = virtualBoxManager.getSessionObject();
		/*
		 * If set to Write, then attempt to acquire an exclusive write lock or fail. If
		 * set to Shared, then either acquire an exclusive write lock or establish a
		 * link to an existing session.
		 */
		machine.lockMachine(session, LockType.Shared);

		IProgress powerDown = session.getConsole().powerDown();
		progressStatus(virtualBoxManager, powerDown, 10000);

	}
	
	public boolean progressStatus(VirtualBoxManager mgr, IProgress p, long waitMillis) {
        long end = System.currentTimeMillis() + waitMillis;
        while (!p.getCompleted()) {
                // process system event queue
            /*
            doc says:
            It is good practice in long-running API clients to process the system events every now and then
            in the main thread (does not work in other threads). As a rule of thumb it makes sense to process
            them every few 100msec to every few seconds). This is done by calling
             */
            mgr.waitForEvents(0);

            /*
            doc:
            Waits until the task is done (including all sub-operations) with a given timeout in milliseconds;
            specify -1 for an indefinite wait.
             */
            
            p.waitForCompletion(-1);
            
            if (System.currentTimeMillis() >= end) {
                return false;
            }
        }
        //to check progress status
        System.out.println(p.getDescription());
        if (p.getErrorInfo() != null) {
            System.out.println(p.getErrorInfo().getText());
        }
        return true;
    }

}
