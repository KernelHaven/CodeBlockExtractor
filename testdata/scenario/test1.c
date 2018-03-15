#include <stdio.h>
#include "logging.h"

void calc(int operand1, int operand2) {
    #if defined(CONFIG_ADDITION) && (!defined(CONFIG_A))
        int result = operand1 + operand2;
        char op = '+';
    #elif defined(CONFIG_SUBTRACTION)
        int result = operand1 - operand2;
        char op = '-';
    #endif
    
    printf("%i %c %i = %i\n", operand1, op, operand2, result);
}

int main(int argc, char **argv) {
    printf("Hello World\n");
    
    #ifdef CONFIG_LOGGING
    LOG("Debbuging this example");
    #endif
    
    #ifdef CONFIG_CALCULATION
    calc(73, 37);
    #endif
    
    return 0;
}
