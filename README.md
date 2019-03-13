# Golflin

Supports:
- Boolean (f,t)
- Numbers
- String/Char (can be created with "text")
- Lists (can be created with [element~element~element])

## Example programs:


### Sum all numbers in a [string](https://codegolf.stackexchange.com/questions/181060/sum-integers-in-a-string)
Arguments: "123"
Program: MN;S

*Why it works*
```
  M       map chars
   N      transform a char to its corresponding number (0 if it's not 1..9)   
    ;     end mapping
     S    sum the integers in a list 
```
