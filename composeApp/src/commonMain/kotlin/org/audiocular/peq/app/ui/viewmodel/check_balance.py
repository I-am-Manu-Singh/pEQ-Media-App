
with open('AppViewModel.kt', 'r') as f:
    balance = 0
    for i, line in enumerate(f, 1):
        for char in line:
            if char == '{':
                balance += 1
            elif char == '}':
                balance -= 1
        if balance < 0:
            print(f"ERROR: Balance went negative at line {i}")
            break
    print(f"Final balance: {balance}")
