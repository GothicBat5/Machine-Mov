#include <linux/acpi.h>
#include <linux/mm.h>
#include <linux/types.h>
#include <asm/cpu_ops.h>

struct parking_protocol_mailbox {
	__le32 cpu_id;
	__le32 reserved;
	__le64 entry_point;
};

struct cpu_mailbox_entry {
	struct parking_protocol_mailbox __iomem *mailbox;
	phys_addr_t mailbox_addr;
	u8 version;
	u8 gic_cpu_id;
};

static struct cpu_mailbox_entry cpu_mailbox_entries[NR_CPUS];

void __init acpi_set_mailbox_entry(int cpu, struct acpi_madt_generic_interrupt *p)
{
	struct cpu_mailbox_entry *cpu_entry = &cpu_mailbox_entries[cpu];

	cpu_entry->mailbox_addr = p->parked_address;
	cpu_entry->version = p->parking_version;
	cpu_entry->gic_cpu_id = p->cpu_interface_number;
}

bool acpi_parking_protocol_valid(int cpu)
{
	struct cpu_mailbox_entry *cpu_entry = &cpu_mailbox_entries[cpu];

	return cpu_entry->mailbox_addr && cpu_entry->version;
}

static int acpi_parking_protocol_cpu_init(unsigned int cpu)
{
	pr_debug("%s: ACPI parked addr=%llx\n", __func__, cpu_mailbox_entries[cpu].mailbox_addr);

	return 0;
}

static int acpi_parking_protocol_cpu_prepare(unsigned int cpu)
{
	return 0;
}

static int acpi_parking_protocol_cpu_boot(unsigned int cpu)
{
	struct cpu_mailbox_entry *cpu_entry = &cpu_mailbox_entries[cpu];
	struct parking_protocol_mailbox __iomem *mailbox;
	u32 cpu_id;

	mailbox = ioremap(cpu_entry->mailbox_addr, sizeof(*mailbox));
  
	if (!mailbox) return -EIO;

	cpu_id = readl_relaxed(&mailbox->cpu_id);

	if (cpu_id != ~0U) 
  {
		iounmap(mailbox);
		return -ENXIO;
	}

	cpu_entry->mailbox = mailbox;
	writeq_relaxed(__pa_symbol(secondary_entry), &mailbox->entry_point);
	writel_relaxed(cpu_entry->gic_cpu_id, &mailbox->cpu_id);

	arch_send_wakeup_ipi(cpu);

	return 0;
}

static void acpi_parking_protocol_cpu_postboot(void)
{
	int cpu = smp_processor_id();
	struct cpu_mailbox_entry *cpu_entry = &cpu_mailbox_entries[cpu];
	struct parking_protocol_mailbox __iomem *mailbox = cpu_entry->mailbox;
	u64 entry_point;
	entry_point = readq_relaxed(&mailbox->entry_point);

	WARN_ON(entry_point);
}

const struct cpu_operations acpi_parking_protocol_ops = {
	.name		= "parking-protocol",
	.cpu_init	= acpi_parking_protocol_cpu_init,
	.cpu_prepare	= acpi_parking_protocol_cpu_prepare,
	.cpu_boot	= acpi_parking_protocol_cpu_boot,
	.cpu_postboot	= acpi_parking_protocol_cpu_postboot
};
