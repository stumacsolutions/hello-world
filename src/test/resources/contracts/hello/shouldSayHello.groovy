package hello

org.springframework.cloud.contract.spec.Contract.make {
    request {
        method 'GET'
        url '/'
    }
    response {
        status 200
        body('Hello, World.')
    }
}
