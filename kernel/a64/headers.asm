	.macro	efi_signature_nop
#ifdef CONFIG_EFI
.L_head:
	ccmp	x18, #0, #0xd, pl
#else
	nop
#endif
	.endm
	.macro	__EFI_PE_HEADER
#ifdef CONFIG_EFI
	.set	.Lpe_header_offset, . - .L_head
	.long	IMAGE_NT_SIGNATURE
	.short	IMAGE_FILE_MACHINE_ARM64	
	.short	.Lsection_count				
	.long	0 			
	.long	0				
	.long	0					
	.short	.Lsection_table - .Loptional_header
	.short	IMAGE_FILE_DEBUG_STRIPPED | \
		IMAGE_FILE_EXECUTABLE_IMAGE | \
		IMAGE_FILE_LINE_NUMS_STRIPPED		
.Loptional_header:
	.short	IMAGE_NT_OPTIONAL_HDR64_MAGIC	
	.byte	0x02					
	.byte	0x14				
	.long	__initdata_begin - .Lefi_header_end	
	.long	__pecoff_data_size		
	.long	0					
	.long	__efistub_efi_pe_entry - .L_head
	.long	.Lefi_header_end - .L_head	
	.quad	0				
	.long	SEGMENT_ALIGN		
	.long	PECOFF_FILE_ALIGNMENT		
	.short	0		
	.short	0				
	.short	LINUX_EFISTUB_MAJOR_VERSION		
	.short	LINUX_EFISTUB_MINOR_VERSION	
	.short	0					
	.short	0				
	.long	0				
	.long	_end - .L_head		
	.long	.Lefi_header_end - .L_head	
	.long	0				
	.short	IMAGE_SUBSYSTEM_EFI_APPLICATION	
	.short	IMAGE_DLLCHARACTERISTICS_NX_COMPAT	
	.quad	0					// SizeOfStackReserve
	.quad	0					// SizeOfStackCommit
	.quad	0					// SizeOfHeapReserve
	.quad	0					// SizeOfHeapCommit
	.long	0					// LoaderFlags
	.long	(.Lsection_table - .) / 8		// NumberOfRvaAndSizes
	.quad	0					// ExportTable
	.quad	0					// ImportTable
	.quad	0					// ResourceTable
	.quad	0					// ExceptionTable
	.quad	0					// CertificationTable
	.quad	0					// BaseRelocationTable

#if defined(CONFIG_DEBUG_EFI) || defined(CONFIG_ARM64_BTI_KERNEL)
	.long	.Lefi_debug_table - .L_head		// DebugTable
	.long	.Lefi_debug_table_size
	__INITRODATA

	.align	2
.Lefi_debug_table:
#ifdef CONFIG_DEBUG_EFI
	.long	0					// Characteristics
	.long	0					// TimeDateStamp
	.short	0					// MajorVersion
	.short	0					// MinorVersion
	.long	IMAGE_DEBUG_TYPE_CODEVIEW		// Type
	.long	.Lefi_debug_entry_size			// SizeOfData
	.long	0					// RVA
	.long	.Lefi_debug_entry - .L_head		// FileOffset
#endif
#ifdef CONFIG_ARM64_BTI_KERNEL
	.long	0					// Characteristics
	.long	0					// TimeDateStamp
	.short	0					// MajorVersion
	.short	0					// MinorVersion
	.long	IMAGE_DEBUG_TYPE_EX_DLLCHARACTERISTICS	// Type
	.long	4					// SizeOfData
	.long	0					// RVA
	.long	.Lefi_dll_characteristics_ex - .L_head	// FileOffset
#endif
	.set	.Lefi_debug_table_size, . - .Lefi_debug_table
	.previous
#endif
.Lsection_table:
	.ascii	".text\0\0\0"
	.long	__initdata_begin - .Lefi_header_end	// VirtualSize
	.long	.Lefi_header_end - .L_head		// VirtualAddress
	.long	__initdata_begin - .Lefi_header_end	// SizeOfRawData
	.long	.Lefi_header_end - .L_head		// PointerToRawData
	.long	0					// PointerToRelocations
	.long	0					// PointerToLineNumbers
	.short	0					// NumberOfRelocations
	.short	0					// NumberOfLineNumbers
	.long	IMAGE_SCN_CNT_CODE | \
		IMAGE_SCN_MEM_READ | \
		IMAGE_SCN_MEM_EXECUTE			// Characteristics
	.ascii	".data\0\0\0"
	.long	__pecoff_data_size			// VirtualSize
	.long	__initdata_begin - .L_head		// VirtualAddress
	.long	__pecoff_data_rawsize			// SizeOfRawData
	.long	__initdata_begin - .L_head		// PointerToRawData
	.long	0					// PointerToRelocations
	.long	0					// PointerToLineNumbers
	.short	0					// NumberOfRelocations
	.short	0					// NumberOfLineNumbers
	.long	IMAGE_SCN_CNT_INITIALIZED_DATA | \
		IMAGE_SCN_MEM_READ | \
		IMAGE_SCN_MEM_WRITE			// Characteristics
	.set	.Lsection_count, (. - .Lsection_table) / 40

#ifdef CONFIG_DEBUG_EFI
.Lefi_debug_entry:
	// EFI_IMAGE_DEBUG_CODEVIEW_NB10_ENTRY
	.ascii	"NB10"					// Signature
	.long	0					// Unknown
	.long	0					// Unknown2
	.long	0					// Unknown3
	.asciz	VMLINUX_PATH
	.set	.Lefi_debug_entry_size, . - .Lefi_debug_entry
#endif
#ifdef CONFIG_ARM64_BTI_KERNEL
.Lefi_dll_characteristics_ex:
	.long	IMAGE_DLLCHARACTERISTICS_EX_FORWARD_CFI_COMPAT
#endif
	.balign	SEGMENT_ALIGN
.Lefi_header_end:
#else
	.set	.Lpe_header_offset, 0x0
#endif
	.endm
