
# Motiion Take-Home Assignment

## Architectural Overview

The system allows users to create a set of validations in json, to be applied to a sequence of arbitrary attributes that appear on a Food Unit. The `ValidationSet` must be created first, where the `id` is the `productType` of the respective `FoodUnit`.

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

It says that the `CuppingScore` must be greater than 1 and the `treeVariety` must be `Arabica`.  You can have any number of validations and they will all be applied.

Now if a user submits a `FoodUnit` json object with `productType` equal to `coffee`, the above validations will be run.  For example:

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
would cresult in the creation of a new `FoodUnit`.

**Note that only the json fields in the `attributes` element will be validated.**

If there were errors during creation, they will be returned to the user. Otherwise, the `FoodUnit` will be created and the generated `id` will be returned.

Here is an overview of the sequence of steps that occur during a create:

![Validation Flow](Validation%20Flow.png "Creation Flow")

## Rest Endpoints
### ValidationSetRoutes
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

### FoodUnitRoutes

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

`PUT /food-units/<id>` - upsert `FoodUnit`.

`GET /food-units/<id>`

`DELETE /food-units/<id>`

### FoodUnit Locations

`GET /food-units/<id>/locations` - get a list of all locations for a `FoodUnit`

`GET /food-units/<id>/locations?maxResult=<max>` - get a list of locations with `<max>` results.  The list is in descending order sorted by `createdDate` so to see the latest, set `<max>` to 1.

`POST /food-units/<id>/locations` - add a new Location 
Body:
```
{
 "longitude":34.1,
 "latitude":45.2,
 "createdDate": "2018-09-25T17:59:43.188Z" //optional. Defaults to now
}
```

### Technologies Used
- Akka Http
- Couchbase (because I had all the code already :-) )
- Cats - for its Validation api
- ScalaTest

### Assumptions & Simplifications
* The validation dsl is very basic and only supports the following:
  * `$eq` for equality of numbers and strings   
  * `$gt`, `$gte`, `$lt`, `$lte` for comparing numbers
* Validations are self-contained and do not rely on external data.
* A `ValidationSet` must be created prior to the creation of a FoodUnit.
* The identifier for the ValidationSet is the `productType` in the `FoodUnit`.
* Nested expressions are not supported.

### How to Run the Tests
**Only couchbase runs in docker. The subsequent `sbt` command will run the code locally.**

From the command line:
```
>docker-compose up -d
>sbt test
```

### How to Run the app
```
>docker-compose up -d
>sbt run
```

### Next Steps
If this were a real project and given my assumptions are valid, I would do a few things:
1. Expand the DSL to contain a richer set of expressions.  For example, we would want logical operations and to be able to support nested expressions.
2. Investigate some other alternatives that would allow us to express validation rules dynamically.  Some candidates would be a templating library like FreeMarker or even vanilla JavaScript.
3. An api that would build the ValidationSet based on a vendor's requirements.
