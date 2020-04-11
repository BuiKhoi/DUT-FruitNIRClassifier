# File tasks:
    main - Main funciton of uvicorn

# Done tasks:


# Todo:
    Check an issue of postman cannot process POST call

# Tutorial:
    The input spectrum has to be an array with shape (227, 1), normalize by (1, 0)
    Every element has to be rounded by 3, eg: 0.113
    Elements of the string are splitted by 'x', eg: 0.113x0.042x0.331x....

    ##Run API
    Setup firewall:
        sudo apt install ufw
        sudo ufw allow 22, 8888, 8000, 5900 <- These are the ports you are using
    Start API: uvicorn --port 8000 --host 0.0.0.0 main:app --reload
    * Where 8000 is your port
    Assume the IP of the computer running the API is 192.168.0.11
    On client computer, API call should be called like the following path: 192.168.0.11:8000/predict/?intensity=<intensity value>