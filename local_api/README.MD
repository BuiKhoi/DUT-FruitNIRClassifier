# File tasks:
    main - Main funciton of uvicorn

# Done tasks:
    - Predicting function ready
    - Collecting data function ready


# Todo:
    - New file with configuration for api, no need to modify main.py each modification

# Tutorial:
## Run API
    Setup firewall:
        sudo apt install ufw
        sudo ufw allow 22, 8888, 8000, 5900 <- These are the ports you are using
    Start API: uvicorn --port 8000 --host 0.0.0.0 main:app --reload
    * Where 8000 is your port
    Assume the IP of the computer running the API is 192.168.0.11
    On client computer, API call should be called like the following path: 192.168.0.11:8000/predict/?intensity=<intensity value>

## intensity input:
    * str
    - standar scaler with mean = 0 and standar deviation = 1
    - rounded by 3
    - concatenate as string, split every elements by 'x'
    - numbers that < 1, remove the zero, eg: 0.113 -> .113
    - eg intensity: '.114x.123x1.355x.....x.198'

## fruit type:
    * str
    - lowercase
    - space are replaced with '_'
    - eg fruit: dragon_fruit, mango

## measure place:
    * int
    - 0: top
    - 1: middle
    - 2: bottom
    - -1: not known

## measure index:
    * int
    - specify the measure index

## sensor id:
    * int
    - id of the sensor used for measuring

## bought date:
    * str
    - the date of buying
    - format by: 'yymmdd'

# sub type:
    * int
    - subtype of a fruit

# API Functions:
    - /predict/: 
        method: POST
        input argument: intensity
        usage: predict what kind of that fruit
    - /collect/:
        method: POST
        input argument: intensity, fruit type, measure place, measure index, sensor id, bought date, sub type
        usage: collect intensity data with its metadata
