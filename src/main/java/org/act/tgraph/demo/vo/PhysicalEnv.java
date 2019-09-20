package org.act.tgraph.demo.vo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.act.tgraph.demo.Config;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.VirtualMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessSort;
import oshi.util.FormatUtil;
import oshi.util.Util;

public enum PhysicalEnv {
    SJH("Intel(R) Core(TM) i5-4570", 3.20, 2, 24, "win10", "");



    String cpu;
    double cpuFreq;
    int cpuPhysicalCoreCnt;
    int physicalMem;
    String description;
    String os;

    PhysicalEnv(String cpu, double cpuFreq, int cpuPhysicalCoreCnt, int physicalMem, String os, String description) {
        this.cpu = cpu;
        this.cpuFreq = cpuFreq;
        this.cpuPhysicalCoreCnt = cpuPhysicalCoreCnt;
        this.physicalMem = physicalMem;
        this.description = description;
        this.os = os;
    }

//    public static String computerInfo(){
//
//    }

    private static StringBuilder oshi = new StringBuilder();
    public static void main(String[] args) {

        System.out.println("Initializing System...");
        SystemInfo si = new SystemInfo();

        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        printOperatingSystem(os);

        System.out.println("Checking computer system...");
        printComputerSystem(hal.getComputerSystem());

        System.out.println("Checking Processor...");
        printProcessor(hal.getProcessor());

        System.out.println("Checking Memory...");
        printMemory(hal.getMemory());

        System.out.println("Checking CPU...");
        printCpu(hal.getProcessor());

        System.out.println("Checking Processes...");
        printProcesses(os, hal.getMemory());

        System.out.println("Checking Sensors...");
        printSensors(hal.getSensors());

        System.out.println("Checking Power sources...");
        printPowerSources(hal.getPowerSources());

        System.out.println("Checking Disks...");
        printDisks(hal.getDiskStores());

        System.out.println("Checking File System...");
        printFileSystem(os.getFileSystem());

        System.out.println("Checking Network interfaces...");
        printNetworkInterfaces(hal.getNetworkIFs());

        System.out.println("Checking Network parameters...");
        printNetworkParameters(os.getNetworkParams());

        // hardware: displays
        System.out.println("Checking Displays...");
        printDisplays(hal.getDisplays());

        // hardware: USB devices
        System.out.println("Checking USB Devices...");
        printUsbDevices(hal.getUsbDevices(true));

        System.out.println("Checking Sound Cards...");
        printSoundCards(hal.getSoundCards());

        System.out.println("Printing Operating System and Hardware Info:\n" + oshi);
    }

    private static void printOperatingSystem(final OperatingSystem os) {
        oshi.append(String.valueOf(os));
        oshi.append("Booted: " + Instant.ofEpochSecond(os.getSystemBootTime()));
        oshi.append("Uptime: " + FormatUtil.formatElapsedSecs(os.getSystemUptime()));
        oshi.append("Running with" + (os.isElevated() ? "" : "out") + " elevated permissions.");
    }

    private static void printComputerSystem(final ComputerSystem computerSystem) {
        oshi.append("system: " + computerSystem.toString());
        oshi.append(" firmware: " + computerSystem.getFirmware().toString());
        oshi.append(" baseboard: " + computerSystem.getBaseboard().toString());
    }

    private static void printProcessor(CentralProcessor processor) {
        oshi.append(processor.toString());
    }

    private static void printMemory(GlobalMemory memory) {
        oshi.append("Memory(total/available): " + memory.getTotal()+"/"+memory.getAvailable());
        VirtualMemory vm = memory.getVirtualMemory();
        oshi.append("Swap(total/used):" + vm.getSwapTotal()+"/"+vm.getSwapUsed());
    }

    private static void printCpu(CentralProcessor processor) {
        oshi.append("Context Switches/Interrupts: " + processor.getContextSwitches() + " / " + processor.getInterrupts());

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        long[][] prevProcTicks = processor.getProcessorCpuLoadTicks();
        oshi.append("CPU, IOWait, and IRQ ticks @ 0 sec:" + Arrays.toString(prevTicks));
        // Wait a second...
        Util.sleep(1000);
        long[] ticks = processor.getSystemCpuLoadTicks();
        oshi.append("CPU, IOWait, and IRQ ticks @ 1 sec:" + Arrays.toString(ticks));
        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

        oshi.append(String.format(
                "User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%% IOwait: %.1f%% IRQ: %.1f%% SoftIRQ: %.1f%% Steal: %.1f%%",
                100d * user / totalCpu, 100d * nice / totalCpu, 100d * sys / totalCpu, 100d * idle / totalCpu,
                100d * iowait / totalCpu, 100d * irq / totalCpu, 100d * softirq / totalCpu, 100d * steal / totalCpu));
        oshi.append(String.format("CPU load: %.1f%%", processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100));
        double[] loadAverage = processor.getSystemLoadAverage(3);
        oshi.append("CPU load averages:" + (loadAverage[0] < 0 ? " N/A" : String.format(" %.2f", loadAverage[0]))
                + (loadAverage[1] < 0 ? " N/A" : String.format(" %.2f", loadAverage[1]))
                + (loadAverage[2] < 0 ? " N/A" : String.format(" %.2f", loadAverage[2])));
        // per core CPU
        StringBuilder procCpu = new StringBuilder("CPU load per processor:");
        double[] load = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
        for (double avg : load) {
            procCpu.append(String.format(" %.1f%%", avg * 100));
        }
        oshi.append(procCpu.toString());
        long freq = processor.getVendorFreq();
        if (freq > 0) {
            oshi.append("Vendor Frequency: " + FormatUtil.formatHertz(freq));
        }
        freq = processor.getMaxFreq();
        if (freq > 0) {
            oshi.append("Max Frequency: " + FormatUtil.formatHertz(freq));
        }
        long[] freqs = processor.getCurrentFreq();
        if (freqs[0] > 0) {
            StringBuilder sb = new StringBuilder("Current Frequencies: ");
            for (int i = 0; i < freqs.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(FormatUtil.formatHertz(freqs[i]));
            }
            oshi.append(sb.toString());
        }
    }

    private static void printProcesses(OperatingSystem os, GlobalMemory memory) {
        oshi.append("Processes: " + os.getProcessCount() + ", Threads: " + os.getThreadCount());
        // Sort by highest CPU
        List<OSProcess> procs = Arrays.asList(os.getProcesses(5, ProcessSort.CPU));

        oshi.append("   PID  %CPU %MEM       VSZ       RSS Name");
        for (int i = 0; i < procs.size() && i < 5; i++) {
            OSProcess p = procs.get(i);
            oshi.append(String.format(" %5d %5.1f %4.1f %9s %9s %s", p.getProcessID(),
                    100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(),
                    100d * p.getResidentSetSize() / memory.getTotal(), FormatUtil.formatBytes(p.getVirtualSize()),
                    FormatUtil.formatBytes(p.getResidentSetSize()), p.getName()));
        }
    }

    private static void printSensors(Sensors sensors) {
        oshi.append("Sensors:\n");
        oshi.append(" cpu temperature:" + sensors.getCpuTemperature());
        oshi.append(" cpu voltage:" + sensors.getCpuVoltage());
        oshi.append(" cpu fan speed:" + Arrays.toString(sensors.getFanSpeeds()));
    }

    private static void printPowerSources(PowerSource[] powerSources) {
        StringBuilder sb = new StringBuilder("Power Sources: ");
        if (powerSources.length == 0) {
            sb.append("Unknown");
        }
        for (PowerSource powerSource : powerSources) {
            sb.append("\n ").append(powerSource.toString());
        }
        oshi.append(sb.toString());
    }

    private static void printDisks(HWDiskStore[] diskStores) {
        oshi.append("Disks:");
        for (HWDiskStore disk : diskStores) {
            oshi.append(" " + disk.getName() + " " + disk.getModel() + " "+disk.getSerial()+" "+
                    "Transfer time: "+disk.getTransferTime()+" "+
                    "Queue length: "+disk.getCurrentQueueLength()+" "+
                    "Read Bytes: "+disk.getReadBytes()+" "+
                    "Reads: "+disk.getReads()+" "+
                    "Write Bytes: "+disk.getWriteBytes()+" "+
                    "Writes: "+disk.getWrites()+" "+
                    "");

            HWPartition[] partitions = disk.getPartitions();
            for (HWPartition part : partitions) {
                oshi.append(" |-- " + part.toString());
            }
        }

    }

    private static void printFileSystem(FileSystem fileSystem) {
        oshi.append("File System:");

        oshi.append(String.format(" File Descriptors: %d/%d", fileSystem.getOpenFileDescriptors(),
                fileSystem.getMaxFileDescriptors()));

        OSFileStore[] fsArray = fileSystem.getFileStores();
        for (OSFileStore fs : fsArray) {
            long usable = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            oshi.append(String.format(
                    " %s (%s) [%s] %s of %s free (%.1f%%), %s of %s files free (%.1f%%) is %s "
                            + (fs.getLogicalVolume() != null && fs.getLogicalVolume().length() > 0 ? "[%s]" : "%s")
                            + " and is mounted at %s",
                    fs.getName(), fs.getDescription().isEmpty() ? "file system" : fs.getDescription(), fs.getType(),
                    FormatUtil.formatBytes(usable), FormatUtil.formatBytes(fs.getTotalSpace()), 100d * usable / total,
                    FormatUtil.formatValue(fs.getFreeInodes(), ""), FormatUtil.formatValue(fs.getTotalInodes(), ""),
                    100d * fs.getFreeInodes() / fs.getTotalInodes(), fs.getVolume(), fs.getLogicalVolume(),
                    fs.getMount()));
        }
    }

    private static void printNetworkInterfaces(NetworkIF[] networkIFs) {
        StringBuilder sb = new StringBuilder("Network Interfaces:");
        if (networkIFs.length == 0) {
            sb.append(" Unknown");
        }
        for (NetworkIF net : networkIFs) {
            sb.append("\n ").append(net.getName()+" Speed:" + net.getSpeed());
        }
        oshi.append(sb.toString());
    }

    private static void printNetworkParameters(NetworkParams networkParams) {
        oshi.append("Network parameters:\n " + networkParams.toString());
    }

    private static void printDisplays(Display[] displays) {
        oshi.append("Displays:");
        int i = 0;
        for (Display display : displays) {
            oshi.append(" Display " + i + ":");
            oshi.append(String.valueOf(display));
            i++;
        }
    }

    private static void printUsbDevices(UsbDevice[] usbDevices) {
        oshi.append("USB Devices:");
        for (UsbDevice usbDevice : usbDevices) {
            oshi.append(String.valueOf(usbDevice));
        }
    }

    private static void printSoundCards(SoundCard[] cards) {
        oshi.append("Sound Cards:");
        for (SoundCard card : cards) {
            oshi.append(" " + String.valueOf(card.getName()));
        }
    }

}
