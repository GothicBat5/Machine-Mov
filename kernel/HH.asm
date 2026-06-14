; A super simple bootloader 
; Assembles to 512 bytes so BIOS can boot it

org 0x7C00          ; BIOS loads us at this address
bits 16             ; Start in 16-bit real mode

start:
    cli             ; Disable interrupts while we set up
    mov ax, 0x07C0  ; Set up segments
    mov ds, ax
    mov es, ax
    
    mov si, msg     ; SI = address of our message
    call print      ; Print it

hang:
    hlt             ; Stop CPU
    jmp hang        ; Loop forever

print:
    mov ah, 0x0E    ; BIOS teletype function
.loop:
    lodsb           ; Load next character
    cmp al, 0       ; End of string?
    je .done
    int 0x10        ; Print character
    jmp .loop
.done:
    ret

msg db 'Sample Made!', 0

; Pad with zeros until byte 510
times 510-($-$$) db 0
; Boot signature - BIOS needs this
dw 0xAA55
