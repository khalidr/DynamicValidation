
# Motiion Take-Home Assignment

## Architectural Overview

The system allows users to create a rule set in Json, to be applied to a sequence of arbitrary attributes that appear on a Food Unit. The `ValidationSet` must be created first, where the `id` is the `productType`
that the validations need to be applied to.

Here is an example of what a `ValidationSet` may look like:

```
{
  "id": "coffee",
  "validations": [
    {
      "targetAttribute": "CuppingScore",
      "validation": {
        "$gt": 1
      }
    },
    {
      "targetAttribute": "treeVariety",
      "validation": {
        "$eq": "Arabica"
      }
    }
  ]
}
```

So we are saying that the `CuppingScore` must be greater than 1 and the `treeVariety` must be `Arabica`.  You can have any number of validations and they will all get executed.

Now if a user submits a `FoodUnit` json object with `productType` equal to `coffee`, it will be first run through these validations.  Here is an example:

```
{
  "owner" : "acmeCoffee",
  "productType" : "coffee",
  "unitDescription" : "honey roasted",
  "mass" : 1.1,
  "expiryDate" : "2018-09-25T03:45:49.788Z",
  "attributes" : {
    "CuppingScore" : 2,
    "treeVariety" : "Arabica"
  }
}
```

**Note that only the json fields in the `attributes` element will be validated.**

If there were errors during creation, they will be returned to the user. Otherwise, the `FoodUnit` will be created and the associated `id` will be returned.


Here is an overview of the sequence of steps that occur during a create:

![Validation Flow](Validation%20Flow.png "Creation Flow")

## Rest Endpoints
### Validations
`GET /validations`

`POST /validations`

Body:
```
    {
      "id": "coffee",
      "validations": [
        {
          "targetAttribute": "CuppingScore",
          "validation": {
            "$gt": 1
          }
        },
        {
          "targetAttribute": "treeVariety",
          "validation": {
            "$eq": "Arabica"
          }
        }
      ]
    }
```

`DELETE /validations/<id>`

### FoodUnit

`POST /food-units`

Body:
```
{
  "owner" : "acmeCoffee",
  "productType" : "coffee",
  "unitDescription" : "honey roasted",
  "mass" : 1.1,
  "expiryDate" : "2018-09-25T03:45:49.788Z",
  "attributes" : {
    "CuppingScore" : 2,
    "treeVariety" : "Arabica"
  },
  "locations":[{
                "longitude":34.1,
                "latitude":45.2
               }]
}
```

`PUT /food-units/<id>` - With `FoodUnit` json payload.

`GET /food-units/<id>`

`DELETE /food-units/<id>`

### FoodUnit Locations

`GET /food-units/<id>/locations` - get a list of all locations for a `FoodUnit`

`GET /food-units/<id>/locations?maxResult=<max>` - get a list of locations with `<max>` results.  The list is in descending order by `createdDate` so to see the latest, set `<max>` to 1.

`POST /food-units/<id>/locations` - add a new Location 


### Technologies Used
- Akka Http
- Couchbase (because I had all the code already :-) )
- Cats - for its Validation api
- ScalaTest

### Assumptions & Simplifations
1. The validation dsl is very straight forward and only supports the following:
  * `$eq` for equality of numbers and strings   
  * `$gt`, `$gte`, `$lt`, `$lte` for comparing numbers
2. Validations are self-contained and do not rely on external data.
3. A validationSet to be used, it must be created prior to the creation of a FoodUnit.
4. The identifier for the ValidationSet is the `productType` in the `FoodUnit`.
    




### Get the Code
`https://github.com/khalidr/motiion.git`

### How to Run the Tests

```
docker-compose up -d
sbt test
```

